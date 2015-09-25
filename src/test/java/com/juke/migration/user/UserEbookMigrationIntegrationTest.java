package com.juke.migration.user;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import com.juke.migration.user.download.DownloadResultWriter;
import com.juke.migration.user.download.EbookMigrationDownloader;
import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;

import java.io.File;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Integration test, using {@link UserEbookMigrator} as a starting point.
 * <p/>
 * This test will start a WireMock-based mock server that emulates our book cover/binary source server to download from.
 *
 * @author Philipp Kumar
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class)
public class UserEbookMigrationIntegrationTest {

    private static final String TESTFILE_VALID_BOOKS = "/validBooks.csv";
    private static final String TESTFILE_LOTS_OF_VALID_BOOKS = "/lotsOfValidBooks.csv";
    private static final String TESTFILE_VALID_BOOK = "/validBook.csv";
    private static final String TESTFILE_INCOMPLETE_BOOK = "/incompleteBook.csv";

    private static final File TEST_RESULTS_FILE = new File(System.getProperty("java.io.tmpdir") + "/ebookMigrationTestResults.csv");
    private static final File TEST_CACHE_DIR = new File(System.getProperty("java.io.tmpdir") + "/ebookMigrationTestCache");

    @Configuration
    static class ContextConfiguration {
        @Bean
        public RestOperations restOperations() {
            return new RestTemplate();
        }

        @Bean
        public UserEbookMigrator userEbookMigrator() {
            return new UserEbookMigrator();
        }

        @Bean
        public EbookMigrationDownloader ebookMigrationDownloader() {
            return new EbookMigrationDownloader();
        }

        @Bean
        public DownloadResultWriter downloadResultWriter() {
            if (TEST_RESULTS_FILE.exists()) {
                if (!TEST_RESULTS_FILE.delete())
                    throw new RuntimeException("could not delete file: " + TEST_RESULTS_FILE.getAbsolutePath());
            }
            return new DownloadResultWriter(TEST_RESULTS_FILE);
        }

        /**
         * Uses java.io.tmpdir for the cache.
         */
        @Bean
        public DownloadCache cache() {
            DownloadCache result = new DownloadCache(TEST_CACHE_DIR);
            result.clear();
            return result;
        }
    }

    /**
     * Port of our source server mock.
     * Don't change this port, it is hardwired in the test files.
     */
    private static final int WIREMOCK_PORT = 8089;

    /**
     * service under test
     */
    @Autowired
    public UserEbookMigrator migrator;

    @Autowired
    public DownloadCache cache;

    @Autowired
    public RestOperations restOps;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(WIREMOCK_PORT);

    @Before
    public void setUp() throws Exception {
        clear();
    }

    @After
    public void tearDown() throws Exception {
    }

    /**
     * A simple test to see if the wiremock setup is working correctly.
     */
    @Test
    @Category(FastTest.class)
    public void wireMockIsWorkingCorrectly() throws Exception {
        final String ping = "ping";
        final String pong = "pong";

        assertThat(restOps, notNullValue());
        stubFor(get(urlEqualTo("/" + ping))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/plain")
                        .withBody(pong)));
        String result = restOps.getForObject(getExternalHostMock() + "/" + ping, String.class);
        assertThat(result, CoreMatchers.equalTo(pong));

        verify(getRequestedFor(urlEqualTo("/" + ping)));
    }


    @Test
    @Category(FastTest.class)
    public void importIncompleteBookFails() throws Exception {
        migrator.migrate(getClass().getResource(TESTFILE_INCOMPLETE_BOOK));

        // the incorrect input csv will lead to exactly 0 requests happening
        verify(0, getRequestedFor(urlPathMatching("/.*")));
    }

    @Test
    @Category(FastTest.class)
    public void canImportValidBooks() throws Exception {
        // set up mock responses for binary downloads
        for (int i = 0; i < 6; i++) {
            stubFor(get(urlPathMatching("/download/" + i + "/.*"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/force-download")
                            .withBodyFile(i + "/binary")));
        }

        // post our JSON to the REST service under test
        migrator.migrate(getClass().getResource(TESTFILE_VALID_BOOKS));

        // verify that our logic downloads books

        verify(getRequestedFor(urlEqualTo(
                "/download/0/?token=abcdef")));
        verify(getRequestedFor(urlEqualTo(
                "/download/1/?token=bcdefa")));
        verify(getRequestedFor(urlEqualTo(
                "/download/2/?token=cdefab")));
        verify(getRequestedFor(urlEqualTo(
                "/download/3/?token=defabc")));
        verify(getRequestedFor(urlEqualTo(
                "/download/4/?token=efabcd")));
        verify(getRequestedFor(urlEqualTo(
                "/download/5/?token=fabcde")));
    }

    @Test
    @Category(FastTest.class)
    public void canDownloadOnRedirect() throws Exception {
        // set up mock response for download after redirect
        final String redirectPathBook = "/download/redirect/thebook";

        stubFor(get(urlPathEqualTo(redirectPathBook))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/force-download")
                        .withBodyFile("0/binary")));


        stubFor(get(urlPathMatching("/download/0/.*"))
                .willReturn(aResponse()
                        .withStatus(302)
                        .withHeader("Location", getExternalHostMock() + redirectPathBook)));

        // call service under test
        migrator.migrate(getClass().getResource(TESTFILE_VALID_BOOK));

        // verify that our logic downloaded binary
        verify(getRequestedFor(urlEqualTo("/download/0/?token=abcdef")));
        verify(getRequestedFor(urlEqualTo(redirectPathBook)));
    }

    @Test
    @Category(FastTest.class)
    public void canDownloadOnHtmlWaitPage() throws Exception {
        String scenario = "waitScenario";
        String stateTryAgain = "TRY_AGAIN";
        String stateDownloadReady = "DOWNLOAD_READY";

        // set up mock responses for binary download
        stubFor(get(urlPathMatching("/download/0/.*"))
                .inScenario(scenario)
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/html")
                        .withBodyFile("wait-html.html"))
                .willSetStateTo(stateTryAgain));

        stubFor(get(urlPathMatching("/download/0/.*"))
                .inScenario(scenario)
                .whenScenarioStateIs(stateTryAgain)
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/html")
                        .withBodyFile("wait-html.html"))
                .willSetStateTo(stateDownloadReady));

        stubFor(get(urlPathMatching("/download/0/.*"))
                .inScenario(scenario)
                .whenScenarioStateIs(stateDownloadReady)
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/force-download")
                        .withBodyFile("0/binary")));

        // call service under test
        migrator.migrate(getClass().getResource(TESTFILE_VALID_BOOK));

        // verify that our logic attempted download twice
        verify(3, getRequestedFor(urlEqualTo(
                "/download/0/?token=abcdef")));
    }

    @Test
    @Category(FastTest.class)
    public void handleDownloadsExceededCorrectly() throws Exception {
        // set up mock responses for binary download
        stubFor(get(urlPathMatching("/download/0/.*"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "text/html")
                        .withBodyFile("download-limit-exceeded.html")));

        // call service under test
        migrator.migrate(getClass().getResource(TESTFILE_VALID_BOOK));

        // verify that our logic attempted download twice
        verify(getRequestedFor(urlEqualTo(
                "/download/0/?token=abcdef")));
    }

    @Test
    @Category(SlowTest.class)
    public void canImportLotsOfValidBooks() throws Exception {
        // set up mock responses for binary downloads
        stubFor(get(urlPathMatching("/download/.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/force-download")
                        .withBodyFile("0/binary")));

        // post our JSON to the REST service under test
        migrator.migrate(getClass().getResource(TESTFILE_LOTS_OF_VALID_BOOKS));

        // verify that our logic downlaods books
        verify(10524, getRequestedFor(urlPathMatching("/download/.*")));
    }

    private void clear() {
        if (TEST_RESULTS_FILE.exists()) {
            if (!TEST_RESULTS_FILE.delete())
                throw new RuntimeException("could not delete file: " + TEST_RESULTS_FILE.getAbsolutePath());
        }
        cache.clear();
    }

    private static String getExternalHostMock() {
        return "http://localhost:" + WIREMOCK_PORT;
    }

}
