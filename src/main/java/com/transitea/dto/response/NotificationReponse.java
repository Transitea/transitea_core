package com.transitea.dto.response;

import com.transitea.entity.enums.StatutNotification;
import com.transitea.entity.enums.TypeCanal;

import java.time.LocalDateTime;

public record NotificationReponse(
        Long id,
        Long colisId,
        String codeTracking,
        String destinataireContact,
        String cible,
        TypeCanal typeCanal,
        String message,
        StatutNotification statut,
        Integer nbTentatives,
        LocalDateTime dateCreation
) {
}
