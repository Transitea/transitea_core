package com.transitea.service.impl;

import com.transitea.entity.Agence;
import com.transitea.entity.Enseigne;
import com.transitea.repository.EnseigneRepository;
import com.transitea.service.QuotaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class QuotaServiceImpl implements QuotaService {

    private static final Logger journal = LoggerFactory.getLogger(QuotaServiceImpl.class);

    private static final double SEUIL_ALERTE = 0.8;

    private final EnseigneRepository enseigneRepository;

    public QuotaServiceImpl(EnseigneRepository enseigneRepository) {
        this.enseigneRepository = enseigneRepository;
    }

    @Override
    public void enregistrerColis(Agence agence) {
        Enseigne enseigne = agence.getEnseigne();

        int avant = enseigne.getColisMoisCourant();
        int apres = avant + 1;
        enseigne.setColisMoisCourant(apres);
        enseigneRepository.save(enseigne);

        if (enseigne.getQuotaColisMois() == null || enseigne.getQuotaColisMois() <= 0) {
            return;
        }

        double ratioAvant = (double) avant / enseigne.getQuotaColisMois();
        double ratioApres = (double) apres / enseigne.getQuotaColisMois();

        if (ratioAvant < 1.0 && ratioApres >= 1.0) {
            journal.warn("Quota mensuel atteint (100%) pour l'enseigne {} : {}/{} colis",
                    enseigne.getNom(), apres, enseigne.getQuotaColisMois());
        } else if (ratioAvant < SEUIL_ALERTE && ratioApres >= SEUIL_ALERTE) {
            journal.warn("Quota mensuel a 80% pour l'enseigne {} : {}/{} colis",
                    enseigne.getNom(), apres, enseigne.getQuotaColisMois());
        }
    }
}
