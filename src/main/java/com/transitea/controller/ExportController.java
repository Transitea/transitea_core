package com.transitea.controller;

import com.transitea.entity.Utilisateur;
import com.transitea.service.ExportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/v1/export")
public class ExportController {

    private final ExportService exportService;

    public ExportController(ExportService exportService) {
        this.exportService = exportService;
    }

    @GetMapping(value = "/colis", produces = "text/csv")
    public ResponseEntity<byte[]> exporterColis(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin,
            @AuthenticationPrincipal Utilisateur utilisateurConnecte) {

        LocalDateTime debutDt = debut.atStartOfDay();
        LocalDateTime finDt = fin.atTime(23, 59, 59);

        byte[] csv = exportService.exporterCsvColis(utilisateurConnecte, debutDt, finDt);

        String nomFichier = "colis_" + debut + "_" + fin + ".csv";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv;charset=UTF-8"));
        headers.setContentDisposition(
                ContentDisposition.attachment().filename(nomFichier).build());

        return ResponseEntity.ok().headers(headers).body(csv);
    }
}
