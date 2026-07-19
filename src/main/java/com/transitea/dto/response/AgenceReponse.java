package com.transitea.dto.response;

import java.time.LocalDateTime;

public record AgenceReponse(
        Long id,
        String uuid,
        String nom,
        String ville,
        String adresse,
        Long enseigneId,
        LocalDateTime dateCreation
) {
}
