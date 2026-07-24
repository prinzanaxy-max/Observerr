package com.backend.observerr.auth.service;

public interface RefreshTokenBlocklistService {

    void blocklist(String jti, long ttlSeconds);

    boolean isBlocked(String jti);
}
