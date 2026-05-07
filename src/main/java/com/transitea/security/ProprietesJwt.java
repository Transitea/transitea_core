package com.transitea.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "application.jwt")
public record ProprietesJwt(
        String secretAccess,
        String secretRefresh,
        long expirationAccessMs,
        long expirationRefreshMs
) {
}
