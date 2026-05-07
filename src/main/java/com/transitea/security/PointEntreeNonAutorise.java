package com.transitea.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transitea.dto.response.ErreurReponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class PointEntreeNonAutorise implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public PointEntreeNonAutorise(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(
            HttpServletRequest requete,
            HttpServletResponse reponse,
            AuthenticationException authException) throws IOException {

        reponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
        reponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        ErreurReponse erreur = new ErreurReponse(
                HttpStatus.UNAUTHORIZED.value(),
                "Token d'acces manquant ou invalide",
                requete.getRequestURI()
        );

        objectMapper.writeValue(reponse.getOutputStream(), erreur);
    }
}
