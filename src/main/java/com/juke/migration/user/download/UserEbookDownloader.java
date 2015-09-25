package com.juke.migration.user.download;

import com.bookpac.utils.logging.ReaktorLogger;
import com.juke.migration.user.dto.UserEbook;
import com.juke.migration.user.task.UserEbookDownloadTask;
import com.juke.migration.user.util.NotifyingBlockingThreadPoolExecutor;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserEbookDownloader {

    private static final ReaktorLogger LOG = ReaktorLogger.getLogger(UserEbookDownloader.class);

    /**
     * max number of parallel tasks in the migration
     */
    public static final int QUEUE_SIZE = 10;

    /**
     * numer of threads used for parallel tasks
     */
    public static final int THREAD_POOL_SIZE = 5;

    /**
     * max time that the migrator will block and await termination of migration tasks
     */
    public static final long MIGRATION_TIMEOUT = TimeUnit.HOURS.toMillis(12);

    @Autowired
    private EbookMigrationDownloader downloader;

    @Autowired
    private DownloadResultWriter resultWriter;

    public void downloadEbooks(List<UserEbook> ebooks) {
        try (DownloadResultWriter writer = resultWriter.open()) {
            // create an executor to perform downloads in parallel
            NotifyingBlockingThreadPoolExecutor downloadExecutor = new NotifyingBlockingThreadPoolExecutor(
                    THREAD_POOL_SIZE, QUEUE_SIZE, 1, TimeUnit.MINUTES);

            for (UserEbook ebook : ebooks) {
                // execute the task.
                // this will block if we already have more tasks in the queue than it can hold.
                downloadExecutor.execute(new UserEbookDownloadTask(downloader, writer, ebook));
            }

            // ask for shutdown
            downloadExecutor.shutdown();
            // block until all tasks have finished. if the timeout is reached before, we just continue
            // (i.e. whole application will shut down without finishing migration)
            try {
                downloadExecutor.awaitTermination(MIGRATION_TIMEOUT, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                LOG.error("interrupted while awaiting thread termination.");
            }
        }
    }
}
