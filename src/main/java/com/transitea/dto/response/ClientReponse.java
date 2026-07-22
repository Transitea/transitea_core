package com.transitea.dto.response;

public record ClientReponse(
        String nom,
        String telephone,
        String ville,
        long nombreColis
) {
}
