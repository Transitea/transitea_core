package com.transitea.controller;

import com.transitea.dto.request.SyncUploadRequete;
import com.transitea.dto.response.SyncDownloadReponse;
import com.transitea.dto.response.SyncUploadReponse;
import com.transitea.entity.Utilisateur;
import com.transitea.service.SyncService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/v1/sync")
public class SyncController {

    private final SyncService syncService;

    public SyncController(SyncService syncService) {
        this.syncService = syncService;
    }

    @PostMapping("/upload")
    public ResponseEntity<SyncUploadReponse> upload(
            @Valid @RequestBody SyncUploadRequete requete,
            @AuthenticationPrincipal Utilisateur utilisateurConnecte) {

        SyncUploadReponse reponse = syncService.synchroniserUpload(requete, utilisateurConnecte);
        return ResponseEntity.ok(reponse);
    }

    @GetMapping("/download")
    public ResponseEntity<SyncDownloadReponse> download(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since,
            @AuthenticationPrincipal Utilisateur utilisateurConnecte) {

        SyncDownloadReponse reponse = syncService.synchroniserDownload(since, utilisateurConnecte);
        return ResponseEntity.ok(reponse);
    }
}
