package com.transitea.service.impl;

import com.transitea.dto.response.MiseAJourStatutReponse;
import com.transitea.dto.response.SuiviPublicReponse;
import com.transitea.entity.Colis;
import com.transitea.entity.MiseAJourStatut;
import com.transitea.exception.EntiteNonTrouveeException;
import com.transitea.mapper.ColisMapper;
import com.transitea.repository.ColisRepository;
import com.transitea.repository.MiseAJourStatutRepository;
import com.transitea.service.TrackingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class TrackingServiceImpl implements TrackingService {

    private final ColisRepository colisRepository;
    private final MiseAJourStatutRepository miseAJourStatutRepository;
    private final ColisMapper colisMapper;

    public TrackingServiceImpl(
            ColisRepository colisRepository,
            MiseAJourStatutRepository miseAJourStatutRepository,
            ColisMapper colisMapper) {
        this.colisRepository = colisRepository;
        this.miseAJourStatutRepository = miseAJourStatutRepository;
        this.colisMapper = colisMapper;
    }

    @Override
    public SuiviPublicReponse suivreParCode(String codeTracking) {
        Colis colis = colisRepository.findByCodeTrackingAndSupprimeFalse(codeTracking)
                .orElseThrow(() -> new EntiteNonTrouveeException("Colis", codeTracking));

        List<MiseAJourStatut> historique =
                miseAJourStatutRepository.findByColisOrderByDateCreationAsc(colis);

        List<MiseAJourStatutReponse> historiqueReponse =
                colisMapper.versMiseAJourReponses(historique);

        return new SuiviPublicReponse(
                colis.getCodeTracking(),
                colis.getExpediteurNom(),
                colis.getDestinataireNom(),
                colis.getDestinataireVille(),
                colis.getDescription(),
                colis.getPoids(),
                colis.getStatutActuel(),
                colis.getDateCreation(),
                historiqueReponse
        );
    }
}
