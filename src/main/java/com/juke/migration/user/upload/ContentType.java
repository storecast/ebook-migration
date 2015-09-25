package com.juke.migration.user.upload;

public enum ContentType {
    ASCM("application/vnd.adobe.adept+xml"), EPUB("application/epub+zip"), PDF("application/pdf");

    private final String mimeType;

    ContentType(String mimeType) {
        this.mimeType = mimeType;
    }

    public static ContentType getByMimeType(final String mimeType){
        for (ContentType contentType : values()) {
            if (contentType.mimeType.equals(mimeType)) {
                return contentType;
            }
        }

        throw new IllegalArgumentException("unexpected mime type " + mimeType);
    }
}
