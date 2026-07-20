package com.transitea.dto.response;

import com.transitea.entity.enums.PalierAbonnement;
import com.transitea.entity.enums.StatutEnseigne;

public record EnseigneReponse(
        Long id,
        String nom,
        PalierAbonnement palierAbonnement,
        Integer quotaColisMois,
        Integer colisMoisCourant,
        double pourcentageConsomme,
        StatutEnseigne statut
) {
}
