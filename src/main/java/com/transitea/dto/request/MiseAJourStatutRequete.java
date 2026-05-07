package com.transitea.dto.request;

import com.transitea.entity.enums.StatutColis;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MiseAJourStatutRequete(

        @NotNull(message = "Le statut est obligatoire")
        StatutColis statut,

        @Size(max = 150, message = "La localisation ne peut pas depasser 150 caracteres")
        String localisation,

        @Size(max = 500, message = "Le commentaire ne peut pas depasser 500 caracteres")
        String commentaire
) {
}
