package com.juke.migration.user.util;

import com.bookpac.server.user.IWSUserMgmt;
import com.bookpac.server.user.WSTUser;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.juke.migration.user.Constants;
import com.juke.migration.user.dto.UserEbook;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserCache {

    @Autowired
    private IWSUserMgmt userMgmt;

    private final LoadingCache<String, Long> userCache = CacheBuilder.newBuilder().build(new CacheLoader<String, Long>() {
        @Override
        public Long load(String email) throws Exception {
            List<WSTUser> users = userMgmt.searchUsers(Constants.ADMIN_TOKEN, ImmutableMap.of("userEmail", email, "nature", "msh.de"), 0, 2).getEntries();
            if (users.size() != 1) {
                throw new RuntimeException("found " + users.size() + " users for email " + email + "; " + users);
            }

            return users.get(0).getUserID();
        }
    });

    public long getUserId(final UserEbook ebook) {
        return userCache.getUnchecked(ebook.getUserEmail());
    }

}
