package com.juke.migration.user.metadata;

import com.bookpac.server.document.DocumentAttribute;
import com.bookpac.server.document.IWSDocMgmt;
import com.bookpac.server.document.WSTDocument;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ListMultimap;
import com.juke.migration.user.Constants;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserDocIsbnCache {

    @Autowired
    private IWSDocMgmt docMgmt;

    private final LoadingCache<Long, ListMultimap<String, String>> existingEbookIsbnsCache = CacheBuilder.newBuilder().build(new CacheLoader<Long, ListMultimap<String, String>>() {
        @Override
        public ListMultimap<String, String> load(Long userId) throws Exception {
            List<WSTDocument> documents = docMgmt.getAllDocumentsForUser(Constants.ADMIN_TOKEN, userId, false, 0, -1);
            ImmutableListMultimap.Builder<String, String> builder = ImmutableListMultimap.builder();
            for (WSTDocument document : documents) {
                builder.put(StringUtils.defaultString(document.getAttributes().get(DocumentAttribute.ISBN.name())), document.getDocumentID());
            }
            return builder.build();
        }
    });

    public List<String> getUserDocIds(final long userId, final String isbn) {
        return existingEbookIsbnsCache.getUnchecked(userId).get(isbn);
    }
}
