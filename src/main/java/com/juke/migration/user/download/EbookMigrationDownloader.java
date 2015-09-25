package com.juke.migration.user.download;

import com.bookpac.common.utils.net.http.HttpClientFactory;
import com.bookpac.utils.logging.ReaktorLogger;
import com.bookpac.utils.net.TimeoutType;
import com.google.common.io.Files;
import com.juke.migration.user.DownloadCache;
import com.juke.migration.user.dto.UserEbook;
import com.juke.migration.user.exception.DownloadLimitExceededException;
import com.juke.migration.user.exception.HttpStatusCodeException;
import com.juke.migration.user.util.FileUtil;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Downloads cover and book binary to our {@link DownloadCache}, based on a {@link UserEbook} document.
 *
 * @author Philipp Kumar
 * @author Gregor Zeitlinger
 */
@Service
public class EbookMigrationDownloader {

    private static final ReaktorLogger LOG = ReaktorLogger.getLogger(EbookMigrationDownloader.class);

    public static final int NUM_DOWNLOAD_RETRIES = 100;
    public static final long DELAY_BETWEEN_RETRIES = TimeUnit.SECONDS.toMillis(3);

    @Autowired
    private DownloadCache cache;

    @Autowired
    private FileUtil fileUtil;

    /**
     * Download book cover and binary using given {@link UserEbook} instance.
     */
    public void downloadBook(UserEbook book) throws DownloadLimitExceededException, HttpStatusCodeException, IOException {
        LOG.debug("downloading book: " + book);
        doDownloadBook(book);
    }

    private void doDownloadBook(UserEbook book) throws DownloadLimitExceededException, HttpStatusCodeException, IOException {
        final File binaryFile = fileUtil.getBinary(book);
        if (binaryFile.exists()) {
            LOG.debug("file found in cache, skipping download: " + binaryFile.getAbsolutePath());
            return;
        }

        LOG.debug("starting download: " + book.getBinaryUrl());

        boolean fileDownloaded = false;

        HttpClientFactory httpClientFactory = new HttpClientFactory(TimeoutType.DEFAULT);
        try (CloseableHttpClient client = httpClientFactory.create()) {
            for (int i = 0; !fileDownloaded && i < NUM_DOWNLOAD_RETRIES; i++) {
                HttpResponse response = client.execute(new HttpGet(book.getBinaryUrl()));
                int statusCode = response.getStatusLine().getStatusCode();

                if (statusCode != 200) {
                    String body = EntityUtils.toString(response.getEntity());
                    if (StringUtils.isBlank(body))
                        body = "<body was empty>";

                    EntityUtils.consume(response.getEntity());

                    Files.write(body, fileUtil.getErrorBinary(book), Charset.forName("UTF-8"));

                    writeHeaders(fileUtil.getErrorHeader(book), response, statusCode);

                    // this is really hacky, but there is no other way to detect download limit exceed.
                    // we do not match against the umlaut because that seems even more unstable.
                    if (!StringUtils.isBlank(body) && body.contains("Der Download ist nicht mehr g")) {
                        throw new DownloadLimitExceededException();
                    }

                    // this is an unknown error, but at least we have the status code to propagate
                    throw new HttpStatusCodeException(statusCode);
                }

                // status code is 200

                // treat code 200 with content HTML as the waiting page
                if ("text/html".equals(response.getFirstHeader(HttpHeaders.CONTENT_TYPE).getValue())) {
                    EntityUtils.consume(response.getEntity());

                    try {
                        LOG.debug("Got HTML, waiting a bit and retrying: " + book.getBinaryUrl());
                        Thread.sleep(DELAY_BETWEEN_RETRIES);
                    } catch (InterruptedException e) {
                        // we were interrupted, leave loop
                        break;
                    }
                    continue;
                }

                writeHeaders(fileUtil.getHeader(book), response, statusCode);
                writeBody(binaryFile, response);

                fileDownloaded = true;
                LOG.debug("Finished download: " + book.getBinaryUrl());
            }
        }
    }

    private static void writeBody(File file, HttpResponse response) throws IOException {
        byte[] bytes = asBytes(response);
        Files.write(bytes, file);
    }

    private static void writeHeaders(File file, HttpResponse response, int statusCode) throws IOException {
        Element headers = new Element("headers");
        for (Header header : response.getAllHeaders()) {
            Element element = new Element("header");
            element.addContent(new Element("key").addContent(header.getName()));
            element.addContent(new Element("value").addContent(header.getValue()));
            headers.addContent(element);
        }

        Element root = new Element("root");
        root.addContent(headers);
        root.addContent(new Element("code").addContent(Integer.toString(statusCode)));
        root.addContent(new Element("line").addContent(response.getStatusLine().getReasonPhrase()));

        Document document = new Document(root);
        new XMLOutputter(Format.getPrettyFormat()).output(document,
                new FileOutputStream(file.getAbsolutePath()));
    }

    private static byte[] asBytes(final HttpResponse response) throws IOException {
        if (response.getEntity().getContent() == null) {
            throw new IllegalArgumentException("content is empty");
        }

        final byte[] bytes = EntityUtils.toByteArray(response.getEntity());
        EntityUtils.consume(response.getEntity());

        return bytes;
    }
}
