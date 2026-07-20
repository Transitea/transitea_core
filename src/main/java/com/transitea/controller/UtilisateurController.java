package com.transitea.controller;

import com.transitea.dto.request.CreationUtilisateurRequete;
import com.transitea.dto.request.MiseAJourStatutUtilisateurRequete;
import com.transitea.dto.response.ReponsePagee;
import com.transitea.dto.response.UtilisateurReponse;
import com.transitea.service.UtilisateurService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/utilisateurs")
@PreAuthorize("hasRole('ADMIN')")
public class UtilisateurController {

    private static final int TAILLE_PAGE_PAR_DEFAUT = 20;

    private final UtilisateurService utilisateurService;

    public UtilisateurController(UtilisateurService utilisateurService) {
        this.utilisateurService = utilisateurService;
    }

    @GetMapping
    public ResponseEntity<ReponsePagee<UtilisateurReponse>> lister(
            @RequestParam(required = false) Long agenceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + TAILLE_PAGE_PAR_DEFAUT) int taille) {

        Pageable pageable = PageRequest.of(
                page, taille, Sort.by(Sort.Direction.DESC, "dateCreation"));

        return ResponseEntity.ok(utilisateurService.lister(agenceId, pageable));
    }

    @PostMapping
    public ResponseEntity<UtilisateurReponse> creer(
            @Valid @RequestBody CreationUtilisateurRequete requete) {

        UtilisateurReponse reponse = utilisateurService.creer(requete);
        return ResponseEntity.status(HttpStatus.CREATED).body(reponse);
    }

    @PatchMapping("/{id}/statut")
    public ResponseEntity<UtilisateurReponse> mettreAJourStatut(
            @PathVariable Long id,
            @Valid @RequestBody MiseAJourStatutUtilisateurRequete requete) {

        return ResponseEntity.ok(utilisateurService.mettreAJourStatut(id, requete.statut()));
    }
}
