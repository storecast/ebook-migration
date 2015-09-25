package com.juke.migration.user.upload;

import com.bookpac.utils.logging.ReaktorLogger;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

/**
 * @author Philipp Kumar
 */
public class UploadResultWriter implements AutoCloseable {

    private static final ReaktorLogger LOG = ReaktorLogger.getLogger(UploadResultWriter.class);

    private final File outFile;
    private CSVPrinter printer;

    public UploadResultWriter(File outFile) {
        if (outFile == null)
            throw new IllegalArgumentException("outFile was null");

        this.outFile = outFile;
    }

    public synchronized UploadResultWriter open() {
        LOG.info("Opening result writer on file: " + outFile.getAbsolutePath());

        if (outFile.exists()) {
            throw new RuntimeException("File already exists, stopping: " + outFile.getAbsolutePath());
        }

        checkIfWritable(outFile);

        final FileWriter fileWriter;
        try {
            fileWriter = new FileWriter(outFile, true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            this.printer = new CSVPrinter(fileWriter,
                    CSVFormat.EXCEL
                            .withDelimiter(';')
                            .withHeader("idRef", "isbn", "userEmail", "resultStatus", "errorMessage"));
        } catch (IOException e) {
            throw new RuntimeException("Unable to write results to CSV: " + outFile.getAbsolutePath(), e);
        }

        return this;
    }

    public synchronized void writeResult(UploadResult result) {
        try {
            printer.printRecord(
                    result.ebook.getIdRef(),
                    result.ebook.getIsbn(),
                    result.ebook.getUserEmail(),
                    result.status.name(),
                    result.errorMessage
            );
        } catch (IOException e) {
            LOG.error("Could not write result record, this is really bad! skipping: " + result.ebook, e);
        }
    }


    public synchronized void close() {
        LOG.info("Closing CSV printer... " + printer);

        try {
            printer.close();
        } catch (IOException e) {
            LOG.error("Could not close printer, this is really bad!", e);
        }

    }

    private synchronized void checkIfWritable(File outFile) {
        if (!outFile.exists())
            try {
                if (!outFile.createNewFile())
                    throw new RuntimeException("expected non-existant file: " + outFile.getAbsolutePath());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        if (!outFile.canWrite()) {
            throw new RuntimeException("Cannot write to file: " + outFile.getAbsolutePath());
        }
    }

}
