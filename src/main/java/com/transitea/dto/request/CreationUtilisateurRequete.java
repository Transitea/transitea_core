package com.transitea.dto.request;

import com.transitea.entity.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreationUtilisateurRequete(

        @NotBlank(message = "Le nom est obligatoire")
        @Size(max = 100, message = "Le nom ne peut pas depasser 100 caracteres")
        String nom,

        @NotBlank(message = "Le prenom est obligatoire")
        @Size(max = 100, message = "Le prenom ne peut pas depasser 100 caracteres")
        String prenom,

        @NotBlank(message = "L'email est obligatoire")
        @Email(message = "L'email n'est pas valide")
        @Size(max = 150, message = "L'email ne peut pas depasser 150 caracteres")
        String email,

        @Size(max = 20, message = "Le telephone ne peut pas depasser 20 caracteres")
        String telephone,

        @NotBlank(message = "Le mot de passe est obligatoire")
        @Size(min = 8, max = 100, message = "Le mot de passe doit contenir entre 8 et 100 caracteres")
        String motDePasse,

        @NotNull(message = "Le role est obligatoire")
        Role role,

        @NotNull(message = "L'agence de rattachement est obligatoire")
        Long agenceId
) {
}
