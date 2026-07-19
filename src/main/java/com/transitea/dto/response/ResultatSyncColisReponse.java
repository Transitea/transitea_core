package com.transitea.dto.response;

public record ResultatSyncColisReponse(
        Long localId,
        String codeTracking,
        boolean succes,
        boolean doublon,
        String erreur
) {
}
