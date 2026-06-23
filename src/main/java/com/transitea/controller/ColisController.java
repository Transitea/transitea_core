package com.transitea.controller;

import com.transitea.dto.request.CreationColisRequete;
import com.transitea.dto.request.MiseAJourStatutRequete;
import com.transitea.dto.response.ColisReponse;
import com.transitea.dto.response.ReponsePagee;
import com.transitea.dto.response.StatistiquesReponse;
import com.transitea.entity.Utilisateur;
import com.transitea.entity.enums.StatutColis;
import com.transitea.service.ColisService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/colis")
public class ColisController {

    private static final int TAILLE_PAGE_PAR_DEFAUT = 20;

    private final ColisService colisService;

    public ColisController(ColisService colisService) {
        this.colisService = colisService;
    }

    @PostMapping
    public ResponseEntity<ColisReponse> creer(
            @Valid @RequestBody CreationColisRequete requete,
            @AuthenticationPrincipal Utilisateur utilisateurConnecte) {

        ColisReponse reponse = colisService.creer(requete, utilisateurConnecte);
        return ResponseEntity.status(HttpStatus.CREATED).body(reponse);
    }

    @GetMapping
    public ResponseEntity<ReponsePagee<ColisReponse>> lister(
            @RequestParam(required = false) StatutColis statut,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + TAILLE_PAGE_PAR_DEFAUT) int taille,
            @AuthenticationPrincipal Utilisateur utilisateurConnecte) {

        Pageable pageable = PageRequest.of(
                page, taille, Sort.by(Sort.Direction.DESC, "dateCreation"));

        return ResponseEntity.ok(colisService.lister(utilisateurConnecte, statut, pageable));
    }

    @GetMapping("/recherche")
    public ResponseEntity<ReponsePagee<ColisReponse>> rechercher(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + TAILLE_PAGE_PAR_DEFAUT) int taille,
            @AuthenticationPrincipal Utilisateur utilisateurConnecte) {

        Pageable pageable = PageRequest.of(
                page, taille, Sort.by(Sort.Direction.DESC, "dateCreation"));

        return ResponseEntity.ok(colisService.rechercher(utilisateurConnecte, q, pageable));
    }

    @GetMapping("/statistiques")
    public ResponseEntity<StatistiquesReponse> statistiques(
            @AuthenticationPrincipal Utilisateur utilisateurConnecte) {

        return ResponseEntity.ok(colisService.obtenirStatistiques(utilisateurConnecte));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ColisReponse> trouverParId(
            @PathVariable Long id,
            @AuthenticationPrincipal Utilisateur utilisateurConnecte) {

        return ResponseEntity.ok(colisService.trouverParId(id, utilisateurConnecte));
    }

    @GetMapping(value = "/{id}/qrcode", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> genererQrCode(
            @PathVariable Long id,
            @AuthenticationPrincipal Utilisateur utilisateurConnecte) {

        byte[] qrCode = colisService.genererQrCode(id, utilisateurConnecte);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(qrCode);
    }

    @PatchMapping("/{id}/statut")
    public ResponseEntity<ColisReponse> mettreAJourStatut(
            @PathVariable Long id,
            @Valid @RequestBody MiseAJourStatutRequete requete,
            @AuthenticationPrincipal Utilisateur utilisateurConnecte) {

        return ResponseEntity.ok(colisService.mettreAJourStatut(id, requete, utilisateurConnecte));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimer(
            @PathVariable Long id,
            @AuthenticationPrincipal Utilisateur utilisateurConnecte) {

        colisService.supprimer(id, utilisateurConnecte);
    }
}
