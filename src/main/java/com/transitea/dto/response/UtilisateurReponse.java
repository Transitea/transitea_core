package com.transitea.dto.response;

import com.transitea.entity.enums.Role;
import com.transitea.entity.enums.StatutUtilisateur;

import java.time.LocalDateTime;

public record UtilisateurReponse(
        Long id,
        String uuid,
        String nom,
        String prenom,
        String email,
        String telephone,
        Role role,
        StatutUtilisateur statut,
        Long agenceId,
        String agenceNom,
        LocalDateTime dateCreation
) {
}
