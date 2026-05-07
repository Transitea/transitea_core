package com.transitea.controller;

import com.transitea.dto.response.SuiviPublicReponse;
import com.transitea.service.TrackingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/tracking")
public class TrackingController {

    private final TrackingService trackingService;

    public TrackingController(TrackingService trackingService) {
        this.trackingService = trackingService;
    }

    @GetMapping("/{codeTracking}")
    public ResponseEntity<SuiviPublicReponse> suivre(@PathVariable String codeTracking) {
        return ResponseEntity.ok(trackingService.suivreParCode(codeTracking));
    }
}
