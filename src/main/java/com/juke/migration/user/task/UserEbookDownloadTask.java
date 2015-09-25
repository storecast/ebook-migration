package com.juke.migration.user.task;

import com.bookpac.utils.logging.ReaktorLogger;
import com.juke.migration.user.download.DownloadResult;
import com.juke.migration.user.download.DownloadResultWriter;
import com.juke.migration.user.download.EbookMigrationDownloader;
import com.juke.migration.user.dto.UserEbook;
import com.juke.migration.user.exception.DownloadLimitExceededException;
import com.juke.migration.user.exception.HttpStatusCodeException;

import java.io.IOException;

/**
 * Represents a task to download a single book.
 *
 * @author Philipp Kumar
 */
public class UserEbookDownloadTask implements Runnable {

    private static final ReaktorLogger LOG = ReaktorLogger.getLogger(UserEbookDownloadTask.class);

    private final EbookMigrationDownloader downloader;
    private final DownloadResultWriter resultWriter;

    private final UserEbook eBook;

    public UserEbookDownloadTask(EbookMigrationDownloader downloader, DownloadResultWriter resultWriter, UserEbook eBook) {
        this.downloader = downloader;
        this.resultWriter = resultWriter;
        this.eBook = eBook;
    }

    @Override
    public void run() {
        try {
            downloader.downloadBook(eBook);
        } catch (DownloadLimitExceededException e) {
            resultWriter.writeResult(new DownloadResult(eBook, DownloadStatus.LIMIT_EXCEEDED));
            return;
        } catch (HttpStatusCodeException e) {
            resultWriter.writeResult(new DownloadResult(eBook, DownloadStatus.UNKNOWN_ERROR,
                    "Received HTTP Code " + e.getStatusCode()));
            return;
        } catch (IOException e) {
            resultWriter.writeResult(new DownloadResult(eBook, DownloadStatus.UNKNOWN_ERROR, e.getMessage()));
            return;
        }

        resultWriter.writeResult(new DownloadResult(eBook, DownloadStatus.SUCCESS, ""));
    }

    public enum DownloadStatus {
        SUCCESS,
        LIMIT_EXCEEDED,
        UNKNOWN_ERROR
    }

}
