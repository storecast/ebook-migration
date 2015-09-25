package com.juke.migration.user.dto;

import javax.validation.constraints.NotNull;

/**
 * A representation of an e-book instance/fulfillment that belongs to a specific user.
 *
 * @author Philipp Kumar
 */
public class UserEbook {

    @NotNull
    private final String idRef;

    @NotNull
    private final String isbn;

//    @NotNull
//    private final String coverUrl;

    @NotNull
    private final String binaryUrl;

    @NotNull
    private final String userEmail;

    private final String fileType;

    private final String protectionType;

    private final String errorReason;

    public UserEbook(String idRef, String isbn, String binaryUrl,
                     String userEmail, String fileType, String protectionType,
                     String errorReason) {
        this.idRef = idRef;
        this.isbn = isbn;
        this.binaryUrl = binaryUrl;
        this.userEmail = userEmail;
        this.fileType = fileType;
        this.protectionType = protectionType;
        this.errorReason = errorReason;
    }

    public String getIdRef() {
        return idRef;
    }

    public String getIsbn() {
        return isbn;
    }

    public String getBinaryUrl() {
        return binaryUrl;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public String getFileType() {
        return fileType;
    }

    public String getProtectionType() {
        return protectionType;
    }

    public String getErrorReason() {
        return errorReason;
    }

    @Override
    public String toString() {
        return "UserEbook{" +
                "idRef='" + idRef + '\'' +
                ", isbn='" + isbn + '\'' +
                ", binaryUrl='" + binaryUrl + '\'' +
                ", userEmail='" + userEmail + '\'' +
                ", fileType='" + fileType + '\'' +
                ", protectionType='" + protectionType + '\'' +
                ", errorReason='" + errorReason + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        UserEbook userEbook = (UserEbook) o;

        return idRef.equals(userEbook.idRef);
    }

    @Override
    public int hashCode() {
        return idRef.hashCode();
    }
}
