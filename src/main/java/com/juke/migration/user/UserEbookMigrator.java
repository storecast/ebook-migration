package com.juke.migration.user;

import com.bookpac.utils.logging.ReaktorLogger;
import com.juke.migration.user.download.UserEbookDownloader;
import com.juke.migration.user.dto.UserEbook;
import com.juke.migration.user.metadata.MetaDataEnricher;
import com.juke.migration.user.upload.UserEbookUploader;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Main class to orchestrate a migration of user ebooks.
 *
 * @author Philipp Kumar
 */
@Service
public class UserEbookMigrator {

    private static final ReaktorLogger LOG = ReaktorLogger.getLogger(UserEbookMigrator.class);

    @Autowired
    private UserEbookDownloader downloader;

    @Autowired
    private UserEbookUploader userEbookUploader;

    @Autowired
    private MetaDataEnricher metaDataEnricher;

    public void migrate(URL csvFile) {
        LOG.info("Starting migration...");
        LOG.info("Using CSV input file: " + csvFile);

        // 0) validate the CSV input file
        if (csvFile == null) {
            throw new IllegalArgumentException("csvFile was null");
        }
        final List<UserEbook> ebooks = readCsvFile(csvFile);

        downloader.downloadEbooks(ebooks);
        userEbookUploader.upload(ebooks);
        metaDataEnricher.enrich(ebooks);

        // 2) for each successfully downloaded book, do the following

        // 2.1) check in reaktor if metadata of the book is available in our backend (use isbn for lookup).
        // 2.1a) if not, download the cover (as we won't have it in this case)
        // TODO: implement. see com.bookpac.server.tools.SearchDocuments


        // 2.2) check if the user exists in our backend (criterion unclear).
        // 2.2a) if not, create the user in our backend.
        // TODO: implement

        // 2.3) upload the book and (if not present) the cover
        // TODO: implement

        // 2.4) link the document to the content source document (meta enrichment)
        // TODO: implement
    }



    private List<UserEbook> readCsvFile(URL csvFile) {
        // 1) based on CSV input file, download all book binaries to the local cache
        CSVParser csvParser = createCsvParser(csvFile);

        final List<UserEbook> ebooks = new ArrayList<>();

        for (CSVRecord csvRecord : csvParser) {
            LOG.trace("Working on CSV record: " + csvRecord);

            final String idRef = csvRecord.get("ID ref");
            final String isbn = csvRecord.get("ISBN");
            final String url = csvRecord.get("URL");
            final String userName = csvRecord.get("UserName");
            final String fileType = csvRecord.get("FileType");
            final String protectionType = csvRecord.get("ProtectionType");
            final String errorReason = csvRecord.get("Error reason");

            String validationError = getValidationError(idRef, isbn, url, userName);
            if (validationError == null) {
                ebooks.add(new UserEbook(idRef, isbn, url, userName,
                        fileType, protectionType, errorReason));
            } else {
                LOG.error("Skipping record (idRef: " + idRef + ") because it has errors: " + validationError);
            }
        }
        return ebooks;
    }

    private static CSVParser createCsvParser(URL csvFile) {
        CSVParser parser;
        try {
            parser = CSVParser.parse(csvFile, Charset.forName("UTF-8"),
                    CSVFormat.EXCEL.withDelimiter(';').withHeader());
        } catch (IOException e) {
            throw new RuntimeException("unable to parse CSV from '" + csvFile.toExternalForm() + "'", e);
        }
        return parser;
    }

    private static String getValidationError(String idRef, String isbn, String url, String userName) {
        if (StringUtils.isBlank(idRef)) {
            return "ID ref was missing";
        }
        if (StringUtils.isBlank(isbn)) {
            return "ISBN was missing";
        }
        if (StringUtils.isBlank(url)) {
            return "URL was missing";
        }
        if (StringUtils.isBlank(userName)) {
            return "UserName was missing";
        }

        return null;
    }

}
