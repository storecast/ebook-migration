package com.juke.migration.user.upload;

import com.juke.migration.user.dto.UserEbook;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ContentTypeExtractor {

    @Autowired
    private HeaderCache headerCache;

    public ContentType getContentType(final UserEbook ebook) {
        final String contentType = headerCache.getHeaders(ebook).get("Content-Type");
        if (StringUtils.isNotBlank(contentType)) {
            if (";".equals(contentType)) {
                //a quick hack for two books
                return ContentType.EPUB;
            }
            return ContentType.getByMimeType(contentType);
        }

        throw new RuntimeException("no content type found for " + ebook);
    }
}
