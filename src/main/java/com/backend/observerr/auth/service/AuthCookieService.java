package com.backend.observerr.auth.service;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

@Service
public class AuthCookieService {

    public static final String ACCESS_TOKEN_COOKIE = "accessToken";
    public static final String REFRESH_TOKEN_COOKIE = "refreshToken";

    private final boolean secure;
    private final String sameSite;
    private final String path;
    private final long accessMaxAgeSeconds;
    private final long refreshMaxAgeSeconds;

    public AuthCookieService(
            @Value("${auth.cookie.secure:false}") boolean secure,
            @Value("${auth.cookie.same-site:Lax}") String sameSite,
            @Value("${auth.cookie.path:/}") String path,
            @Value("${auth.cookie.access-max-age:86400}") long accessMaxAgeSeconds,
            @Value("${auth.cookie.refresh-max-age:604800}") long refreshMaxAgeSeconds) {
        this.secure = secure;
        this.sameSite = sameSite;
        this.path = path;
        this.accessMaxAgeSeconds = accessMaxAgeSeconds;
        this.refreshMaxAgeSeconds = refreshMaxAgeSeconds;
    }

    public void setAuthCookies(HttpServletResponse response, String accessToken, String refreshToken) {
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie(ACCESS_TOKEN_COOKIE, accessToken, accessMaxAgeSeconds).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie(REFRESH_TOKEN_COOKIE, refreshToken, refreshMaxAgeSeconds).toString());
    }

    public void clearAuthCookies(HttpServletResponse response) {
        // Must match the exact options used when cookies were set, or clearCookie silently fails.
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie(ACCESS_TOKEN_COOKIE, "", 0).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie(REFRESH_TOKEN_COOKIE, "", 0).toString());
    }

    public String extractRefreshToken(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        for (Cookie cookie : request.getCookies()) {
            if (REFRESH_TOKEN_COOKIE.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private ResponseCookie buildCookie(String name, String value, long maxAgeSeconds) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path(path)
                .maxAge(maxAgeSeconds)
                .build();
    }
}
