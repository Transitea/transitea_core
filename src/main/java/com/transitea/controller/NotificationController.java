package com.transitea.controller;

import com.transitea.dto.response.NotificationReponse;
import com.transitea.dto.response.ReponsePagee;
import com.transitea.entity.Utilisateur;
import com.transitea.service.NotificationService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/notifications")
public class NotificationController {

    private static final int TAILLE_PAGE_PAR_DEFAUT = 20;

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<ReponsePagee<NotificationReponse>> lister(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + TAILLE_PAGE_PAR_DEFAUT) int taille,
            @AuthenticationPrincipal Utilisateur utilisateurConnecte) {

        Pageable pageable = PageRequest.of(page, taille);
        return ResponseEntity.ok(notificationService.lister(utilisateurConnecte, pageable));
    }
}
