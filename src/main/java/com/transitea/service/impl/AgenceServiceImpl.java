package com.transitea.service.impl;

import com.transitea.dto.request.CreationAgenceRequete;
import com.transitea.dto.response.AgenceReponse;
import com.transitea.entity.Agence;
import com.transitea.entity.Enseigne;
import com.transitea.exception.EntiteNonTrouveeException;
import com.transitea.exception.ErreurMetier;
import com.transitea.repository.AgenceRepository;
import com.transitea.repository.EnseigneRepository;
import com.transitea.service.AgenceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class AgenceServiceImpl implements AgenceService {

    private final AgenceRepository agenceRepository;
    private final EnseigneRepository enseigneRepository;

    public AgenceServiceImpl(AgenceRepository agenceRepository, EnseigneRepository enseigneRepository) {
        this.agenceRepository = agenceRepository;
        this.enseigneRepository = enseigneRepository;
    }

    @Override
    public AgenceReponse creer(CreationAgenceRequete requete) {
        Enseigne enseigne = enseigneRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new ErreurMetier("Aucune enseigne configuree"));

        Agence agence = Agence.builder()
                .nom(requete.nom())
                .ville(requete.ville())
                .adresse(requete.adresse())
                .enseigne(enseigne)
                .build();

        return versReponse(agenceRepository.save(agence));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AgenceReponse> lister() {
        return agenceRepository.findBySupprimeFalse().stream()
                .map(this::versReponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AgenceReponse trouverParId(Long id) {
        Agence agence = agenceRepository.findByIdAndSupprimeFalse(id)
                .orElseThrow(() -> new EntiteNonTrouveeException("Agence", id));
        return versReponse(agence);
    }

    private AgenceReponse versReponse(Agence agence) {
        return new AgenceReponse(
                agence.getId(),
                agence.getUuid(),
                agence.getNom(),
                agence.getVille(),
                agence.getAdresse(),
                agence.getEnseigne().getId(),
                agence.getDateCreation()
        );
    }
}
