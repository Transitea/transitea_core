package com.transitea.controller;

import com.transitea.dto.response.EnseigneReponse;
import com.transitea.service.EnseigneService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/enseigne")
@PreAuthorize("hasRole('ADMIN')")
public class EnseigneController {

    private final EnseigneService enseigneService;

    public EnseigneController(EnseigneService enseigneService) {
        this.enseigneService = enseigneService;
    }

    @GetMapping
    public ResponseEntity<EnseigneReponse> obtenir() {
        return ResponseEntity.ok(enseigneService.obtenir());
    }
}
