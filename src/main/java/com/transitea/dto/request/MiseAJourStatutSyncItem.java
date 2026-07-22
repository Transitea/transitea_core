package com.transitea.dto.request;

import com.transitea.entity.enums.StatutColis;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record MiseAJourStatutSyncItem(

        Long colisId,

        Long localId,

        @NotNull(message = "Le nouveau statut est obligatoire")
        StatutColis nouveauStatut,

        @Size(max = 500, message = "Le commentaire ne peut pas depasser 500 caracteres")
        String commentaire,

        Integer baseVersion,

        @NotNull(message = "La date du changement cote client est obligatoire")
        LocalDateTime dateChangementClient
) {
}
