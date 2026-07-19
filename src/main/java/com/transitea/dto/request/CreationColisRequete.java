package com.transitea.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreationColisRequete(

        @NotNull(message = "L'agence d'origine est obligatoire")
        Long agenceOrigineId,

        @NotNull(message = "L'agence de retrait est obligatoire")
        Long agenceRetraitId,

        @NotBlank(message = "Le nom de l'expediteur est obligatoire")
        @Size(max = 100, message = "Le nom de l'expediteur ne peut pas depasser 100 caracteres")
        String expediteurNom,

        @Size(max = 20, message = "Le telephone de l'expediteur ne peut pas depasser 20 caracteres")
        String expediteurTelephone,

        @Size(max = 150, message = "L'email de l'expediteur ne peut pas depasser 150 caracteres")
        String expediteurEmail,

        @NotBlank(message = "Le nom du destinataire est obligatoire")
        @Size(max = 100, message = "Le nom du destinataire ne peut pas depasser 100 caracteres")
        String destinataireNom,

        @Size(max = 20, message = "Le telephone du destinataire ne peut pas depasser 20 caracteres")
        String destinataireTelephone,

        @Size(max = 150, message = "L'email du destinataire ne peut pas depasser 150 caracteres")
        String destinataireEmail,

        @Size(max = 255, message = "L'adresse du destinataire ne peut pas depasser 255 caracteres")
        String destinataireAdresse,

        @Size(max = 100, message = "La ville du destinataire ne peut pas depasser 100 caracteres")
        String destinataireVille,

        @Size(max = 500, message = "La description ne peut pas depasser 500 caracteres")
        String description,

        @DecimalMin(value = "0.001", message = "Le poids doit etre superieur a 0")
        BigDecimal poids,

        Long localId
) {
}
