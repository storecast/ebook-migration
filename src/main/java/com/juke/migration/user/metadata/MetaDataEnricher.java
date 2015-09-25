package com.juke.migration.user.metadata;

import com.bookpac.server.common.error.WSException;
import com.bookpac.server.document.DocumentAttribute;
import com.bookpac.server.document.IWSDocMgmt;
import com.bookpac.server.document.WSTDocument;
import com.bookpac.server.scripting.IWSScripting;
import com.bookpac.server.scripting.WSTScriptingLanguage;
import com.bookpac.utils.logging.ReaktorLogger;
import com.google.common.base.Optional;
import com.juke.migration.user.Constants;
import com.juke.migration.user.dto.UserEbook;
import com.juke.migration.user.util.UserCache;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MetaDataEnricher {

    private static final ReaktorLogger LOG = ReaktorLogger.getLogger(MetaDataEnricher.class);

    @Autowired
    private EnrichmentResultWriter enrichmentResultWriter;

    @Autowired
    private UserCache userCache;

    @Autowired
    private UserDocIsbnCache userDocIsbnCache;

    @Autowired
    private IsbnContentSourceDocCache isbnContentSourceDocCache;

    @Autowired
    private IsbnCatalogDocCache isbnCatalogDocCache;

    @Autowired
    private IWSDocMgmt docMgmt;

    @Autowired
    private IWSScripting scripting;

    public void enrich(final List<UserEbook> ebooks) {
        try (EnrichmentResultWriter writer = enrichmentResultWriter.open()) {
            for (UserEbook ebook : ebooks) {
                try {
                    final List<String> userDocIds = userDocIsbnCache.getUserDocIds(userCache.getUserId(ebook), ebook.getIsbn());
                    if (userDocIds.size() > 0) {
                        for (String userDocId : userDocIds) {
                            if (isNotLinkedToCommercialDocument(userDocId)) {
                                enrich(ebook, userDocId);
                            } else {
                                LOG.info("skipping, already {} linked to commercial doc;  {}", userDocId, ebook);
                            }
                        }
                    } else {
                        LOG.info("skipping, no user doc for {}", ebook);
                    }
                } catch (Exception e) {
                    LOG.error("failed to enrich meta data for " + ebook, e);
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private boolean isNotLinkedToCommercialDocument(String userDocId) throws WSException {
        final WSTDocument document = docMgmt.getDocument(Constants.ADMIN_TOKEN, userDocId);
        final boolean isLinkedToCatalogDoc = document.getAttributes().containsKey(DocumentAttribute.CATALOG_DOCUMENT_ID.name());
        final boolean isLinkedToContentSourceDoc = document.getAttributes().containsKey(DocumentAttribute.CONTENT_SOURCE_DOCUMENT_ID.name());
        return !(isLinkedToCatalogDoc || isLinkedToContentSourceDoc);
    }

    private void enrich(final UserEbook ebook, final String userDocId) throws WSException {
        final Optional<String> catalogDocId = isbnCatalogDocCache.getCatalogDocId(ebook);
        if (catalogDocId.isPresent()) {
            linkToCatalogDocument(ebook, userDocId, catalogDocId.get());
            return;
        }

        final Optional<String> contentSourceDocId = isbnContentSourceDocCache.getContentSourceDocId(ebook);
        if (contentSourceDocId.isPresent()) {
            linkToContentSourceDocument(ebook, userDocId, contentSourceDocId.get());
            return;
        }

        enrichmentResultWriter.writeResult(new EnrichmentResult(ebook, EnrichmentResultStatus.NO_COMMERCIAL_DOC_FOUND));
    }

    private void linkToContentSourceDocument(UserEbook ebook, String userDocId, String contentSourceDocId) throws WSException {
        final String script =
                "userDocUid = com.bookpac.server.uid.Uid.parseUnknown('" + userDocId + "')\n" +
                        "csDocUid = com.bookpac.server.uid.Uid.parseUnknown('" + contentSourceDocId + "')\n" +
                        "ud = userDocMgmt.getDocument(userDocUid)\n" +
                        "csd = contentSourceDocumentMgmt.getNotRemoved(csDocUid)\n" +
                        "ud.setContentSourceDocument(csd)\n" +
                        "userDocMgmt.clearOverriddenContentSourceDocumentAttributes(null, ud)";
        scripting.evaluateScript(Constants.ADMIN_TOKEN, WSTScriptingLanguage.GROOVY, script);

        enrichmentResultWriter.writeResult(new EnrichmentResult(ebook, EnrichmentResultStatus.SUCCESS, contentSourceDocId));

//        Preconditions.checkNotNull(docMgmt.getDocument(Constants.ADMIN_TOKEN, userDocId).getAttributes().get(DocumentAttribute.CONTENT_SOURCE_DOCUMENT_ID.name()), "cs doc id not set");
    }

    private void linkToCatalogDocument(UserEbook ebook, String userDocId, String catalogDocId) throws WSException {
        docMgmt.linkUserDocumentToCatalogDocument(Constants.ADMIN_TOKEN, userDocId, catalogDocId);
        enrichmentResultWriter.writeResult(new EnrichmentResult(ebook, EnrichmentResultStatus.SUCCESS, catalogDocId));

//        Preconditions.checkNotNull(docMgmt.getDocument(Constants.ADMIN_TOKEN, userDocId).getAttributes().get(DocumentAttribute.CATALOG_DOCUMENT_ID.name()), "cat doc id not set");

    }
}
