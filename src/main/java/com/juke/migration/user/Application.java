package com.juke.migration.user;

import com.bookpac.server.document.IWSDocMgmt;
import com.bookpac.server.scripting.IWSScripting;
import com.bookpac.server.search.document.IWSSearchDocument;
import com.bookpac.server.user.IWSUserMgmt;
import com.bookpac.utils.appserver.ApiVersion;
import com.bookpac.utils.appserver.IWSLookup;
import com.bookpac.utils.appserver.JsonLookupBuilder;
import com.bookpac.utils.appserver.ReaktorConfiguration;
import com.juke.migration.user.download.DownloadResultWriter;
import com.juke.migration.user.metadata.EnrichmentResultWriter;
import com.juke.migration.user.upload.UploadResultWriter;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Entry point into the application. Starts an embedded web server.
 *
 * @author Philipp Kumar
 */
public class Application {
    @Configuration
    @ComponentScan
    public static class ApplicationContext {
        private static final String PATH_DOWNLOAD_RESULT_FILE = "results.csv";
        private static final String PATH_UPLOAD_RESULT_FILE = "upload-results.csv";
        private static final String PATH_ENRICHMENT_RESULT_FILE = "enrichment-results.csv";
        public static final String PATH_CACHE_DIR = "ebook-migration-download-cache";

        private final IWSLookup lookup = lookup();

        private IWSLookup lookup(){
            try {
                return new JsonLookupBuilder(new URL(Constants.BASE_URL), ReaktorConfiguration.getInstance().getUserAgentString(getClass().getSimpleName()))
                        .setApiVersion(new ApiVersion("3.4")).build();
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }

        @Bean
        public DownloadResultWriter downloadResultWriter() {
            return new DownloadResultWriter(new File(PATH_DOWNLOAD_RESULT_FILE));
        }

        @Bean
        public UploadResultWriter uploadResultWriter() {
            return new UploadResultWriter(new File(PATH_UPLOAD_RESULT_FILE));
        }

        @Bean
        public EnrichmentResultWriter enrichmentResultWriter() {
            return new EnrichmentResultWriter(new File(PATH_ENRICHMENT_RESULT_FILE));
        }

        @Bean
        public DownloadCache cache() {
            return new DownloadCache(new File(PATH_CACHE_DIR));
        }

        @Bean
        public IWSDocMgmt iwsDocMgmt(){
            return lookup.lookup(IWSDocMgmt.class);
        }

        @Bean
        public IWSUserMgmt iwsUserMgmt() {
            return lookup.lookup(IWSUserMgmt.class);
        }

        @Bean
        public IWSSearchDocument iwsSearchDocument(){
            return lookup.lookup(IWSSearchDocument.class);
        }

        @Bean
        public IWSScripting iwsScripting() {
            return lookup.lookup(IWSScripting.class);
        }
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Single parameter expected: absolute path to csvOutputFile");
            return;
        }

        AnnotationConfigApplicationContext context =
                new AnnotationConfigApplicationContext(ApplicationContext.class);

        try {
            context.getBean(UserEbookMigrator.class).migrate(new URL("file:" + args[0]));
        } catch (MalformedURLException e) {
            System.err.println(e.getMessage());
        }
    }

}
