package com.flight.backend.constants;

public final class SecurityConstants {

    private SecurityConstants() {
        // Prevent instantiation
    }

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";
    public static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";

    // JWT Claim Keys
    public static final String CLAIM_USER_ID = "userId";
    public static final String CLAIM_EMAIL = "email";
    public static final String CLAIM_ROLE = "role";
    public static final String CLAIM_TYPE = "type";

    // Token Types
    public static final String TOKEN_TYPE_ACCESS = "ACCESS";
    public static final String TOKEN_TYPE_REFRESH = "REFRESH";

    // Header metadata
    public static final String HEADER_USER_AGENT = "User-Agent";
    public static final String HEADER_X_FORWARDED_FOR = "X-Forwarded-For";
}
