package com.juke.migration.user.metadata;

import com.bookpac.common.utils.NatureTokens;
import com.bookpac.server.search.document.IWSSearchDocument;
import com.bookpac.server.search.document.WSTSearchDocumentObjectResult;
import com.bookpac.server.search.document.WSTSearchDocumentResult;
import com.google.common.base.Optional;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.juke.migration.user.dto.UserEbook;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class IsbnCatalogDocCache {

    @Autowired
    private IWSSearchDocument searchDocument;

    private final LoadingCache<String, Optional<String>> isbnCatalogDocIdCache = CacheBuilder.newBuilder().build(new CacheLoader<String, Optional<String>>() {
        @Override
        public Optional<String> load(String isbn) throws Exception {
            final List<WSTSearchDocumentResult> list = searchDocument.searchDocuments(NatureTokens.MSH_DE, "isbn:" + isbn, null, 0, 2, null, false, null, null, null).getResults();
            if (list.size() > 1) {
                throw new RuntimeException("wrong number of hits: " + list.size() + " for " + isbn);
            }
            if (list.size() == 1) {
                final WSTSearchDocumentObjectResult result = (WSTSearchDocumentObjectResult) list.get(0);
                return Optional.of(result.getSearchResult().getDocumentID());
            } else {
                return Optional.absent();
            }
        }
    });

    public Optional<String> getCatalogDocId(final UserEbook ebook) {
        return isbnCatalogDocIdCache.getUnchecked(ebook.getIsbn());
    }
}
