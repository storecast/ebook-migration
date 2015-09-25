package com.juke.migration.user.upload;

import com.google.common.base.Optional;
import com.juke.migration.user.dto.UserEbook;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FilenameExtractor {

    @Autowired
    private HeaderCache headerCache;

    public Optional<String> getFilename(final UserEbook ebook){
        final String contentDisposition = headerCache.getHeaders(ebook).get("Content-Disposition");
        return Optional.fromNullable(StringUtils.trimToNull(StringUtils.substringBetween(contentDisposition, "filename=\"", "\"")));
    }

}
