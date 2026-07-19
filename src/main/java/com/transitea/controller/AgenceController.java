package com.transitea.controller;

import com.transitea.dto.request.CreationAgenceRequete;
import com.transitea.dto.response.AgenceReponse;
import com.transitea.service.AgenceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/agences")
public class AgenceController {

    private final AgenceService agenceService;

    public AgenceController(AgenceService agenceService) {
        this.agenceService = agenceService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AgenceReponse> creer(@Valid @RequestBody CreationAgenceRequete requete) {
        return ResponseEntity.status(HttpStatus.CREATED).body(agenceService.creer(requete));
    }

    @GetMapping
    public ResponseEntity<List<AgenceReponse>> lister() {
        return ResponseEntity.ok(agenceService.lister());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AgenceReponse> trouverParId(@PathVariable Long id) {
        return ResponseEntity.ok(agenceService.trouverParId(id));
    }
}
