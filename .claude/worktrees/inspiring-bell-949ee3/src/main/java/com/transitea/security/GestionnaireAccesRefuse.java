package com.transitea.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transitea.dto.response.ErreurReponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class GestionnaireAccesRefuse implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public GestionnaireAccesRefuse(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(
            HttpServletRequest requete,
            HttpServletResponse reponse,
            AccessDeniedException accessDeniedException) throws IOException {

        reponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
        reponse.setStatus(HttpServletResponse.SC_FORBIDDEN);

        ErreurReponse erreur = new ErreurReponse(
                HttpStatus.FORBIDDEN.value(),
                "Acces refuse : droits insuffisants",
                requete.getRequestURI()
        );

        objectMapper.writeValue(reponse.getOutputStream(), erreur);
    }
}
