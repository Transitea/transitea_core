package com.transitea.service.impl;

import com.transitea.dto.response.EnseigneReponse;
import com.transitea.entity.Enseigne;
import com.transitea.exception.ErreurMetier;
import com.transitea.repository.EnseigneRepository;
import com.transitea.service.EnseigneService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class EnseigneServiceImpl implements EnseigneService {

    private final EnseigneRepository enseigneRepository;

    public EnseigneServiceImpl(EnseigneRepository enseigneRepository) {
        this.enseigneRepository = enseigneRepository;
    }

    @Override
    public EnseigneReponse obtenir() {
        Enseigne enseigne = enseigneRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new ErreurMetier("Aucune enseigne configuree"));

        double pourcentage = enseigne.getQuotaColisMois() != null && enseigne.getQuotaColisMois() > 0
                ? (enseigne.getColisMoisCourant() * 100.0) / enseigne.getQuotaColisMois()
                : 0.0;

        return new EnseigneReponse(
                enseigne.getId(),
                enseigne.getNom(),
                enseigne.getPalierAbonnement(),
                enseigne.getQuotaColisMois(),
                enseigne.getColisMoisCourant(),
                pourcentage,
                enseigne.getStatut()
        );
    }
}
