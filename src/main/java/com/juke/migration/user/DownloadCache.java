package com.juke.migration.user;

import com.bookpac.utils.logging.ReaktorLogger;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

/**
 * A file-based cache.
 *
 * @author Philipp Kumar
 */
public class DownloadCache {

    private static final ReaktorLogger LOG = ReaktorLogger.getLogger(DownloadCache.class);

    private final File location;

    public DownloadCache(File location) {
        LOG.info("Using cache dir: " + location.getAbsolutePath());

        File dir = new File(location.getAbsolutePath() + "/testdir");
        if (!dir.exists()) {
            try {
                FileUtils.forceMkdir(dir);
            } catch (IOException e) {
                throw new RuntimeException("directory is not writable, giving up: " + dir.getAbsolutePath());
            }
        }

        this.location = location;
    }

    public File getLocation() {
        return this.location;
    }

    public void clear() {
        LOG.info("Clearing cache dir: " + location.getAbsolutePath());
        if (location.exists()) {
            try {
                FileUtils.forceDelete(location);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
