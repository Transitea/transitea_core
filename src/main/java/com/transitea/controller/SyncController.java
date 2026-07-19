package com.transitea.controller;

import com.transitea.dto.request.SyncRequete;
import com.transitea.dto.response.SyncReponse;
import com.transitea.entity.Utilisateur;
import com.transitea.service.SyncService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/sync")
public class SyncController {

    private final SyncService syncService;

    public SyncController(SyncService syncService) {
        this.syncService = syncService;
    }

    @PostMapping
    public ResponseEntity<SyncReponse> synchroniser(
            @Valid @RequestBody SyncRequete requete,
            @AuthenticationPrincipal Utilisateur utilisateurConnecte) {

        SyncReponse reponse = syncService.synchroniser(requete, utilisateurConnecte);
        return ResponseEntity.ok(reponse);
    }
}
