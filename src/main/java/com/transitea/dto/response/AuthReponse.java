package com.transitea.dto.response;

public record AuthReponse(
        String accessToken,
        String refreshToken,
        String typeToken,
        long expirationAccessMs,
        UtilisateurReponse utilisateur
) {
    public static AuthReponse creer(
            String accessToken,
            String refreshToken,
            long expirationAccessMs,
            UtilisateurReponse utilisateur) {

        return new AuthReponse(accessToken, refreshToken, "Bearer", expirationAccessMs, utilisateur);
    }
}
