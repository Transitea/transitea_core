package com.transitea.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "application.whatsapp")
public record ProprietesWhatsApp(
        String token,
        String phoneNumberId,
        String apiUrl
) {
    public boolean estConfigure() {
        return token != null && !token.isBlank()
                && phoneNumberId != null && !phoneNumberId.isBlank();
    }
}
