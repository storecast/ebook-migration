package com.juke.migration.user;

import com.bookpac.server.common.KnownUserToken;

public interface Constants {

    KnownUserToken ADMIN_TOKEN = new KnownUserToken("<token>");
    String BASE_URL = "http://localhost:8080";
}
