package com.juke.migration.user.upload;

import com.bookpac.server.common.error.WSException;
import com.bookpac.utils.logging.ReaktorLogger;
import com.juke.migration.user.dto.UserEbook;
import com.juke.migration.user.util.FileUtil;
import com.juke.migration.user.util.UserCache;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CheckUploadNecessary {
    private static final ReaktorLogger LOG = ReaktorLogger.getLogger(CheckUploadNecessary.class);

    @Autowired
    private FileUtil fileUtil;

    @Autowired
    private UserCache userCache;

    @Autowired
    private ExistingEbookIsbnsCache existingEbookIsbnsCache;

    @Autowired
    private UploadResultWriter uploadResultWriter;

    public boolean shouldUpload(final UserEbook ebook) throws IOException, WSException {
        return hasValidBinary(ebook) && notAlreadyUploaded(ebook);
    }

    private boolean notAlreadyUploaded(UserEbook ebook) throws WSException {
        final boolean hasIsbn = existingEbookIsbnsCache.hasIsbn(userCache.getUserId(ebook), ebook.getIsbn());
        if (hasIsbn){
            LOG.info("already uploaded {}", ebook);
            //not protocolling this
//            uploadResultWriter.writeResult(new UploadResult(ebook, UploadResultStatus.ALREADY_UPLOADED));
        }
        return !hasIsbn;
    }

    private boolean hasValidBinary(UserEbook ebook) throws IOException {
        final File binary = fileUtil.getBinary(ebook);
        if (! binary.exists()) {
            LOG.info("no binary for {}", ebook);
            uploadResultWriter.writeResult(new UploadResult(ebook, UploadResultStatus.NO_VALID_BINARY, "no binary present"));
            return false;
        }

        final String excludePattern1 = "Dieser Downloadlink ist nicht mehr g";
        final String excludePattern2 = "Download Token has expired";
        final String excludePattern3 = "Die Anzahl der zul";

        final byte[] bytes = new byte[excludePattern1.getBytes().length];
        try (final InputStream in = new FileInputStream(binary)){
            in.read(bytes);
        }

        final boolean hasPattern = StringUtils.startsWithAny(new String(bytes), excludePattern1, excludePattern2, excludePattern3);
        if (hasPattern){
            LOG.info("expired download detected for {}", ebook);
            uploadResultWriter.writeResult(new UploadResult(ebook, UploadResultStatus.NO_VALID_BINARY, "expired download detected"));
        }
        return !hasPattern;
    }
}
