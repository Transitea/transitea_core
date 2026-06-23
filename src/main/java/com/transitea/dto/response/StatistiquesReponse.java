package com.transitea.dto.response;

import com.transitea.entity.enums.StatutColis;

import java.util.Map;

public record StatistiquesReponse(
        long total,
        Map<StatutColis, Long> parStatut
) {
}
