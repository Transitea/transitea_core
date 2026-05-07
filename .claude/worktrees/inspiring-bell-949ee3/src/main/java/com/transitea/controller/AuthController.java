package com.transitea.controller;

import com.transitea.dto.request.ConnexionRequete;
import com.transitea.dto.request.InscriptionRequete;
import com.transitea.dto.request.RafraichissementRequete;
import com.transitea.dto.response.AuthReponse;
import com.transitea.dto.response.UtilisateurReponse;
import com.transitea.entity.Utilisateur;
import com.transitea.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthReponse> register(
            @Valid @RequestBody InscriptionRequete requete) {

        AuthReponse reponse = authService.inscrire(requete);
        return ResponseEntity.status(HttpStatus.CREATED).body(reponse);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthReponse> login(
            @Valid @RequestBody ConnexionRequete requete) {

        AuthReponse reponse = authService.connecter(requete);
        return ResponseEntity.ok(reponse);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthReponse> refresh(
            @Valid @RequestBody RafraichissementRequete requete) {

        AuthReponse reponse = authService.rafraichir(requete);
        return ResponseEntity.ok(reponse);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody RafraichissementRequete requete) {
        authService.deconnecter(requete.refreshToken());
    }

    @GetMapping("/me")
    public ResponseEntity<UtilisateurReponse> getProfile(
            @AuthenticationPrincipal Utilisateur utilisateurConnecte) {

        UtilisateurReponse reponse = authService.obtenirProfil(utilisateurConnecte);
        return ResponseEntity.ok(reponse);
    }
}
