package com.transitea.dto.response;

import com.transitea.entity.enums.StatutColis;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record SuiviPublicReponse(
        String codeTracking,
        String expediteurNom,
        String destinataireNom,
        String destinataireVille,
        String description,
        BigDecimal poids,
        StatutColis statutActuel,
        LocalDateTime dateCreation,
        List<MiseAJourStatutReponse> historique
) {
}
