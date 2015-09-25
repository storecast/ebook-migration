package com.juke.migration.user.metadata;

import com.juke.migration.user.dto.UserEbook;

/**
 * @author Philipp Kumar
 */
public class EnrichmentResult {
    public final UserEbook ebook;
    public final EnrichmentResultStatus status;

    public final String errorMessage;

    public EnrichmentResult(UserEbook ebook, EnrichmentResultStatus status) {
        this(ebook, status, "");
    }

    public EnrichmentResult(UserEbook ebook, EnrichmentResultStatus status, String errorMessage) {
        this.ebook = ebook;
        this.status = status;
        this.errorMessage = errorMessage;
    }
}
