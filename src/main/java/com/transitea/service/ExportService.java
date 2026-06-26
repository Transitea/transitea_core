package com.transitea.service;

import com.transitea.entity.Utilisateur;

import java.time.LocalDateTime;

public interface ExportService {

    byte[] exporterCsvColis(Utilisateur utilisateur, LocalDateTime debut, LocalDateTime fin);
}
