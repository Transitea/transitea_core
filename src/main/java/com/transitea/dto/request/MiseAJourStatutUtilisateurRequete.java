package com.transitea.dto.request;

import com.transitea.entity.enums.StatutUtilisateur;
import jakarta.validation.constraints.NotNull;

public record MiseAJourStatutUtilisateurRequete(

        @NotNull(message = "Le statut est obligatoire")
        StatutUtilisateur statut
) {
}
