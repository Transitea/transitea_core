package com.transitea.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RafraichissementRequete(

        @NotBlank(message = "Le token de rafraichissement est obligatoire")
        String refreshToken
) {
}
