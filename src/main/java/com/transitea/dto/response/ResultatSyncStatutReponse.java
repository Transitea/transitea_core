package com.transitea.dto.response;

import com.transitea.entity.enums.StatutColis;

public record ResultatSyncStatutReponse(
        Long colisId,
        Long localId,
        String codeTracking,
        boolean succes,
        boolean conflit,
        StatutColis statutApplique,
        String erreur
) {
}
