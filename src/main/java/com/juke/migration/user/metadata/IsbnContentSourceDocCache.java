package com.juke.migration.user.metadata;

import com.bookpac.server.document.IWSDocMgmt;
import com.bookpac.server.document.WSTDocument;
import com.google.common.base.Optional;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.juke.migration.user.Constants;
import com.juke.migration.user.dto.UserEbook;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class IsbnContentSourceDocCache {

    @Autowired
    private IWSDocMgmt docMgmt;

    private final LoadingCache<String, Optional<String>> isbnContentSourceDocIdCache = CacheBuilder.newBuilder().build(new CacheLoader<String, Optional<String>>() {
        @Override
        public Optional<String> load(String isbn) throws Exception {
            final List<WSTDocument> contentSourceDocumentsByIsbn = docMgmt.getContentSourceDocumentsByIsbn(Constants.ADMIN_TOKEN, isbn);
            if (contentSourceDocumentsByIsbn.size() > 0) {
                //just pick the first one
                return Optional.of(contentSourceDocumentsByIsbn.get(0).getDocumentID());
            } else {
                return Optional.absent();
            }
        }
    });

    public Optional<String> getContentSourceDocId(final UserEbook ebook) {
        return isbnContentSourceDocIdCache.getUnchecked(ebook.getIsbn());
    }
}
