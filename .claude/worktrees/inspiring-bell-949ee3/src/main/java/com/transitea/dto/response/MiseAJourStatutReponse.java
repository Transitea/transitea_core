package com.transitea.dto.response;

import com.transitea.entity.enums.StatutColis;

import java.time.LocalDateTime;

public record MiseAJourStatutReponse(
        Long id,
        StatutColis statut,
        StatutColis ancienStatut,
        String localisation,
        String commentaire,
        Long utilisateurId,
        LocalDateTime dateCreation
) {
}
