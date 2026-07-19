package com.transitea.dto.response;

import com.transitea.entity.enums.StatutColis;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ColisReponse(
        Long id,
        String uuid,
        String codeTracking,
        Long agenceOrigineId,
        String agenceOrigineNom,
        Long agenceRetraitId,
        String agenceRetraitNom,
        String expediteurNom,
        String expediteurTelephone,
        String expediteurEmail,
        String destinataireNom,
        String destinataireTelephone,
        String destinataireEmail,
        String destinataireAdresse,
        String destinataireVille,
        String description,
        BigDecimal poids,
        StatutColis statutActuel,
        Long localId,
        Integer version,
        LocalDateTime dateCreation,
        List<MiseAJourStatutReponse> historique
) {
}
