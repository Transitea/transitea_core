package com.transitea.controller;

import com.transitea.dto.response.ClientReponse;
import com.transitea.dto.response.ReponsePagee;
import com.transitea.entity.Utilisateur;
import com.transitea.service.ClientService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/clients")
public class ClientController {

    private static final int TAILLE_PAGE_PAR_DEFAUT = 20;

    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    @GetMapping
    public ResponseEntity<ReponsePagee<ClientReponse>> lister(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + TAILLE_PAGE_PAR_DEFAUT) int taille,
            @AuthenticationPrincipal Utilisateur utilisateurConnecte) {

        Pageable pageable = PageRequest.of(page, taille);
        return ResponseEntity.ok(clientService.lister(utilisateurConnecte, pageable));
    }
}
