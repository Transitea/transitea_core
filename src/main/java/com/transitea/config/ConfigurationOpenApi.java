package com.transitea.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConfigurationOpenApi {

    private static final String SCHEME_JWT = "bearerAuth";

    @Bean
    public OpenAPI openApiTransitea() {
        return new OpenAPI()
                .info(new Info()
                        .title("Transitea API")
                        .description("API de gestion et de suivi de colis multi-agences (depot/retrait) "
                                + "pour les enseignes de transport de la diaspora.")
                        .version("v1")
                        .contact(new Contact().name("Transitea")))
                .addSecurityItem(new SecurityRequirement().addList(SCHEME_JWT))
                .components(new Components()
                        .addSecuritySchemes(SCHEME_JWT, new SecurityScheme()
                                .name(SCHEME_JWT)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
