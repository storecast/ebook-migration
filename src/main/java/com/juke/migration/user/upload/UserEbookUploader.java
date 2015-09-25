package com.juke.migration.user.upload;

import com.bookpac.common.utils.net.http.HttpClientFactory;
import com.bookpac.server.common.error.WSException;
import com.bookpac.server.document.IWSDocMgmt;
import com.bookpac.server.scripting.IWSScripting;
import com.bookpac.server.scripting.WSTScriptingLanguage;
import com.bookpac.utils.logging.ReaktorLogger;
import com.bookpac.utils.net.TimeoutType;
import com.google.common.base.Optional;
import com.juke.migration.user.Constants;
import com.juke.migration.user.dto.UserEbook;
import com.juke.migration.user.util.FileUtil;
import com.juke.migration.user.util.UserCache;
import java.io.IOException;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserEbookUploader {
    private static final ReaktorLogger LOG = ReaktorLogger.getLogger(UserEbookUploader.class);

    @Autowired
    private CheckUploadNecessary checkUploadNecessary;

    @Autowired
    private ContentTypeExtractor contentTypeExtractor;

    @Autowired
    private UserCache userCache;

    @Autowired
    private FileUtil fileUtil;

    @Autowired
    private FilenameExtractor filenameExtractor;

    @Autowired
    private IWSScripting scripting;

    @Autowired
    private UploadResultWriter uploadResultWriter;

    @Autowired
    private IWSDocMgmt docMgmt;

    public void upload(final List<UserEbook> ebooks){
        try (UploadResultWriter writer = uploadResultWriter.open()) {
            for (UserEbook ebook : ebooks) {
                try {
                    if (checkUploadNecessary.shouldUpload(ebook)) {
                        final String docid = upload(ebook);
                        writer.writeResult(new UploadResult(ebook, UploadResultStatus.SUCCESS, docid));
                    } else {
                        LOG.info("skipping {} upload not necessary", ebook);
                    }
                } catch (Exception e) {
                    LOG.error("Failed to upload " + ebook, e);
                    writer.writeResult(new UploadResult(ebook, UploadResultStatus.GENERAL_FAILURE, e.getMessage()));
//                throw new RuntimeException(e);
                }
            }
        }
    }

    private String upload(UserEbook ebook) throws WSException, IOException {
        final String docid = doUpload(ebook);
        setISBN(ebook, docid);
        return docid;
    }

    private void setISBN(UserEbook ebook, String docid) throws WSException {
        final String script =
                "uid = com.bookpac.server.uid.Uid.parseUnknown('" + docid + "')\n" +
                "d = userDocMgmt.getDocument(uid)\n" +
                "userDocumentAttributeMgmt.setDocumentAttribute(d, com.bookpac.server.archive.searchfield.DocumentSearchFields.ISBN, '" + ebook.getIsbn() + "')";
        scripting.evaluateScript(Constants.ADMIN_TOKEN, WSTScriptingLanguage.GROOVY, script);

//        Preconditions.checkNotNull(docMgmt.getDocument(Constants.ADMIN_TOKEN, docid).getAttributes().get(DocumentAttribute.ISBN.name()), "ISBN not set");
    }

    private String doUpload(UserEbook ebook) throws IOException {
        final HttpClientFactory httpClientFactory = new HttpClientFactory(TimeoutType.DEFAULT);
        try (final CloseableHttpClient client = httpClientFactory.create()) {
            final HttpPost httpPost = new HttpPost(buildUrl(ebook));
            httpPost.setEntity(new FileEntity(fileUtil.getBinary(ebook)));
            try (final CloseableHttpResponse response = client.execute(httpPost)){
                final int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode != 200) {
                    throw new RuntimeException("upload failed with status " + statusCode + " for " + ebook);
                }

                final String respString = EntityUtils.toString(response.getEntity());
                final String docId;
                if (respString.startsWith("OK ")){
                    docId =  StringUtils.substringAfter(respString, "OK ").trim();
                } else {
                    docId = respString.trim();
                }

                FileUtils.write(fileUtil.getDocId(ebook), docId);
                return docId;
            }
        }
    }

    private String buildUrl(UserEbook ebook) {
        final ContentType contentType = contentTypeExtractor.getContentType(ebook);

        if (contentType == ContentType.ASCM){
            return Constants.BASE_URL + "/delivery/acsmupload?token=" + Constants.ADMIN_TOKEN.getToken() + "&userID=" + userCache.getUserId(ebook);
        } else {
            final Optional<String> filename = filenameExtractor.getFilename(ebook);
            return Constants.BASE_URL + "/delivery/document/upload?token=" + Constants.ADMIN_TOKEN.getToken() + "&userID=" + userCache.getUserId(ebook)
            + (filename.isPresent() ? "&fileName=" + filename.get() : "") + "&format=" + contentType.name();
        }
    }
}
