package com.transitea.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreationAgenceRequete(

        @NotBlank(message = "Le nom de l'agence est obligatoire")
        @Size(max = 100, message = "Le nom de l'agence ne peut pas depasser 100 caracteres")
        String nom,

        @NotBlank(message = "La ville est obligatoire")
        @Size(max = 100, message = "La ville ne peut pas depasser 100 caracteres")
        String ville,

        @Size(max = 255, message = "L'adresse ne peut pas depasser 255 caracteres")
        String adresse
) {
}
