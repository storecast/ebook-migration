package com.juke.migration.user.download;

import com.juke.migration.user.dto.UserEbook;
import com.juke.migration.user.task.UserEbookDownloadTask;

/**
 * @author Philipp Kumar
 */
public class DownloadResult {
    public final UserEbook ebook;
    public final UserEbookDownloadTask.DownloadStatus status;

    public final String errorMessage;

    public DownloadResult(UserEbook ebook, UserEbookDownloadTask.DownloadStatus status) {
        this(ebook, status, "");
    }

    public DownloadResult(UserEbook ebook, UserEbookDownloadTask.DownloadStatus status, String errorMessage) {
        this.ebook = ebook;
        this.status = status;
        this.errorMessage = errorMessage;
    }
}
