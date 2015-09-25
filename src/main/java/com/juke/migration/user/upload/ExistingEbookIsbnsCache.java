package com.juke.migration.user.upload;

import com.bookpac.server.document.DocumentAttribute;
import com.bookpac.server.document.IWSDocMgmt;
import com.bookpac.server.document.WSTDocument;
import com.google.common.base.Function;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.FluentIterable;
import com.juke.migration.user.Constants;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ExistingEbookIsbnsCache {

    @Autowired
    private IWSDocMgmt docMgmt;

    private final LoadingCache<Long, Set<String>> existingEbookIsbnsCache = CacheBuilder.newBuilder().build(new CacheLoader<Long, Set<String>>() {
        @Override
        public Set<String> load(Long userId) throws Exception {
            List<WSTDocument> documents = docMgmt.getAllDocumentsForUser(Constants.ADMIN_TOKEN, userId, false, 0, -1);
            return FluentIterable.from(documents).transform(new Function<WSTDocument, String>() {
                @Override
                public String apply(WSTDocument document) {
                    return StringUtils.defaultString(document.getAttributes().get(DocumentAttribute.ISBN.name()));
                }
            }).toSet();
        }
    });

    public boolean hasIsbn(final long userId, final String isbn) {
        return existingEbookIsbnsCache.getUnchecked(userId).contains(isbn);
    }
}
