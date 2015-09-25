package com.juke.migration.user.util;

import com.bookpac.utils.logging.ReaktorLogger;
import com.juke.migration.user.DownloadCache;
import com.juke.migration.user.dto.UserEbook;
import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FileUtil {
    private static final ReaktorLogger LOG = ReaktorLogger.getLogger(FileUtil.class);

    @Autowired
    private DownloadCache cache;

    public File getBinary(final UserEbook ebook) throws IOException {
        return new File(getDir(ebook), "binary");
    }

    public File getHeader(final UserEbook ebook) throws IOException {
        return new File(getDir(ebook), "binary.headers.xml");
    }

    public File getErrorBinary(final UserEbook ebook) throws IOException {
        return new File(getDir(ebook), "error");
    }

    public File getErrorHeader(final UserEbook ebook) throws IOException {
        return new File(getDir(ebook), "error.headers.xml");
    }

    public File getDocId(final UserEbook ebook) throws IOException {
        return new File(getDir(ebook), "docid.txt");
    }

    private File getDir(UserEbook ebook) throws IOException {
        File dir = new File(cache.getLocation().getAbsolutePath() + "/" + ebook.getIdRef());
        if (!dir.exists()) {
            LOG.debug("Path did not exist, creating: " + dir.getAbsolutePath());
            FileUtils.forceMkdir(dir);
        }
        return dir;
    }
}
