package com.transitea.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class FiltreAuthentificationJwt extends OncePerRequestFilter {

    private static final Logger journal = LoggerFactory.getLogger(FiltreAuthentificationJwt.class);
    private static final String EN_TETE_AUTORISATION = "Authorization";
    private static final String PREFIXE_BEARER = "Bearer ";

    private final ServiceJwt serviceJwt;
    private final ServiceDetailsUtilisateur serviceDetailsUtilisateur;

    public FiltreAuthentificationJwt(
            ServiceJwt serviceJwt,
            ServiceDetailsUtilisateur serviceDetailsUtilisateur) {
        this.serviceJwt = serviceJwt;
        this.serviceDetailsUtilisateur = serviceDetailsUtilisateur;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest requete,
            HttpServletResponse reponse,
            FilterChain chaineFiltres) throws ServletException, IOException {

        String token = extraireToken(requete);

        if (token != null && serviceJwt.validerAccessToken(token)) {
            try {
                String email = serviceJwt.extraireEmailAccessToken(token);

                if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserDetails userDetails = serviceDetailsUtilisateur.loadUserByUsername(email);

                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails, null, userDetails.getAuthorities());

                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(requete));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            } catch (Exception e) {
                journal.warn("Impossible d'authentifier l'utilisateur : {}", e.getMessage());
            }
        }

        chaineFiltres.doFilter(requete, reponse);
    }

    private String extraireToken(HttpServletRequest requete) {
        String entete = requete.getHeader(EN_TETE_AUTORISATION);
        if (entete != null && entete.startsWith(PREFIXE_BEARER)) {
            return entete.substring(PREFIXE_BEARER.length());
        }
        return null;
    }
}
