package com.flight.backend.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;

@Configuration
@ConfigurationProperties(prefix = "jwt")
@Getter
@Setter
public class JwtProperties {

    private String secret;
    private Access access = new Access();
    private Refresh refresh = new Refresh();
    private Cookie cookie = new Cookie();

    @Getter
    @Setter
    public static class Access {
        private long expiration = 900000L; // 15 minutes default in ms
    }

    @Getter
    @Setter
    public static class Refresh {
        private long expiration = 604800000L; // 7 days default in ms
    }

    @Getter
    @Setter
    public static class Cookie {
        private String name = "refreshToken";
        private boolean secure = false;
        private String sameSite = "Lax";
        private String path = "/";
    }
}
