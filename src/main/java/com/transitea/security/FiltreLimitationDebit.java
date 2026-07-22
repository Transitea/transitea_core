package com.transitea.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Limitation de debit simple (fenetre fixe, en memoire) par adresse IP :
 * une limite globale et une limite plus stricte sur la connexion, pour
 * freiner les abus et les attaques par force brute (OWASP A04/A07).
 *
 * Adaptee a un deploiement mono-instance (etat en memoire, non partage
 * entre plusieurs replicas). A remplacer par une solution centralisee
 * (ex : Bucket4j + Redis) en cas de montee en charge horizontale.
 */
@Component
public class FiltreLimitationDebit extends OncePerRequestFilter {

    private static final Logger journal = LoggerFactory.getLogger(FiltreLimitationDebit.class);

    private static final int LIMITE_GLOBALE_PAR_MINUTE = 100;
    private static final int LIMITE_LOGIN_PAR_MINUTE = 10;

    private final ConcurrentHashMap<String, CompteurFenetre> compteursGlobaux = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CompteurFenetre> compteursLogin = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(
            HttpServletRequest requete,
            HttpServletResponse reponse,
            FilterChain chaineFiltres) throws ServletException, IOException {

        String ip = requete.getRemoteAddr();
        boolean estLogin = "POST".equalsIgnoreCase(requete.getMethod())
                && requete.getRequestURI().endsWith("/v1/auth/login");

        if (estLogin && depasseLimite(compteursLogin, ip, LIMITE_LOGIN_PAR_MINUTE)) {
            journal.warn("Limite de tentatives de connexion depassee pour {}", ip);
            envoyerErreurTropDeRequetes(reponse);
            return;
        }

        if (depasseLimite(compteursGlobaux, ip, LIMITE_GLOBALE_PAR_MINUTE)) {
            journal.warn("Limite de requetes globale depassee pour {}", ip);
            envoyerErreurTropDeRequetes(reponse);
            return;
        }

        chaineFiltres.doFilter(requete, reponse);
    }

    private boolean depasseLimite(ConcurrentHashMap<String, CompteurFenetre> compteurs, String cle, int limite) {
        long minuteActuelle = System.currentTimeMillis() / 60_000;
        CompteurFenetre compteur = compteurs.compute(cle, (k, existant) ->
                (existant == null || existant.minute.get() != minuteActuelle)
                        ? new CompteurFenetre(minuteActuelle)
                        : existant);
        return compteur.compte.incrementAndGet() > limite;
    }

    private void envoyerErreurTropDeRequetes(HttpServletResponse reponse) throws IOException {
        reponse.setStatus(429);
        reponse.setContentType("application/json");
        reponse.getWriter().write("{\"statut\":429,\"message\":\"Trop de requetes, veuillez reessayer plus tard\"}");
    }

    private static final class CompteurFenetre {
        private final AtomicLong minute;
        private final AtomicInteger compte = new AtomicInteger(0);

        private CompteurFenetre(long minute) {
            this.minute = new AtomicLong(minute);
        }
    }
}
