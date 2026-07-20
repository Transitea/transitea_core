package com.transitea.service;

import com.transitea.entity.Agence;

public interface QuotaService {

    /**
     * Incremente le compteur mensuel de colis de l'enseigne rattachee a
     * l'agence donnee, et journalise une alerte a 80% et 100% du quota.
     */
    void enregistrerColis(Agence agence);
}
