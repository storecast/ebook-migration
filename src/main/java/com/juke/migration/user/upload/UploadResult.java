package com.juke.migration.user.upload;

import com.juke.migration.user.dto.UserEbook;

/**
 * @author Philipp Kumar
 */
public class UploadResult {
    public final UserEbook ebook;
    public final UploadResultStatus status;

    public final String errorMessage;

    public UploadResult(UserEbook ebook, UploadResultStatus status) {
        this(ebook, status, "");
    }

    public UploadResult(UserEbook ebook, UploadResultStatus status, String errorMessage) {
        this.ebook = ebook;
        this.status = status;
        this.errorMessage = errorMessage;
    }
}
