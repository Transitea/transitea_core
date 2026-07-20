package com.transitea.service.impl;

import com.transitea.dto.request.CreationColisRequete;
import com.transitea.dto.request.SyncRequete;
import com.transitea.dto.response.ResultatSyncColisReponse;
import com.transitea.dto.response.SyncReponse;
import com.transitea.entity.Agence;
import com.transitea.entity.Colis;
import com.transitea.entity.MiseAJourStatut;
import com.transitea.entity.SyncLog;
import com.transitea.entity.Utilisateur;
import com.transitea.entity.enums.StatutColis;
import com.transitea.exception.EntiteNonTrouveeException;
import com.transitea.repository.AgenceRepository;
import com.transitea.repository.ColisRepository;
import com.transitea.repository.MiseAJourStatutRepository;
import com.transitea.repository.SyncLogRepository;
import com.transitea.service.QuotaService;
import com.transitea.service.SyncService;
import com.transitea.util.GenerateurCodeTracking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class SyncServiceImpl implements SyncService {

    private static final Logger journal = LoggerFactory.getLogger(SyncServiceImpl.class);
    private static final int TENTATIVES_MAX_CODE_TRACKING = 5;

    private final ColisRepository colisRepository;
    private final AgenceRepository agenceRepository;
    private final MiseAJourStatutRepository miseAJourStatutRepository;
    private final SyncLogRepository syncLogRepository;
    private final QuotaService quotaService;

    public SyncServiceImpl(
            ColisRepository colisRepository,
            AgenceRepository agenceRepository,
            MiseAJourStatutRepository miseAJourStatutRepository,
            SyncLogRepository syncLogRepository,
            QuotaService quotaService) {
        this.colisRepository = colisRepository;
        this.agenceRepository = agenceRepository;
        this.miseAJourStatutRepository = miseAJourStatutRepository;
        this.syncLogRepository = syncLogRepository;
        this.quotaService = quotaService;
    }

    @Override
    public SyncReponse synchroniser(SyncRequete requete, Utilisateur utilisateur) {
        LocalDateTime dateDebut = LocalDateTime.now();
        List<ResultatSyncColisReponse> resultats = new ArrayList<>();
        int nbReussis = 0;
        int nbDoublons = 0;
        int nbEchecs = 0;

        for (CreationColisRequete colisRequete : requete.colis()) {
            ResultatSyncColisReponse resultat = traiterUnColis(colisRequete, utilisateur);
            resultats.add(resultat);

            if (resultat.doublon()) {
                nbDoublons++;
            } else if (resultat.succes()) {
                nbReussis++;
            } else {
                nbEchecs++;
            }
        }

        enregistrerLog(utilisateur, requete.colis().size(), nbReussis, nbEchecs, dateDebut);

        journal.info("Sync pour {} : {} envoyes, {} reussis, {} doublons, {} echecs",
                utilisateur.getEmail(), requete.colis().size(), nbReussis, nbDoublons, nbEchecs);

        return new SyncReponse(requete.colis().size(), nbReussis, nbDoublons, nbEchecs, resultats);
    }

    private ResultatSyncColisReponse traiterUnColis(
            CreationColisRequete requete, Utilisateur utilisateur) {

        if (requete.localId() != null) {
            var existant = colisRepository.findByCreeParAndLocalIdAndSupprimeFalse(
                    utilisateur, requete.localId());
            if (existant.isPresent()) {
                journal.debug("Doublon detecte pour localId={}", requete.localId());
                return new ResultatSyncColisReponse(
                        requete.localId(),
                        existant.get().getCodeTracking(),
                        true,
                        true,
                        null
                );
            }
        }

        try {
            Agence agenceOrigine = agenceRepository.findByIdAndSupprimeFalse(requete.agenceOrigineId())
                    .orElseThrow(() -> new EntiteNonTrouveeException("Agence", requete.agenceOrigineId()));
            Agence agenceRetrait = agenceRepository.findByIdAndSupprimeFalse(requete.agenceRetraitId())
                    .orElseThrow(() -> new EntiteNonTrouveeException("Agence", requete.agenceRetraitId()));

            String codeTracking = genererCodeTrackingUnique();

            Colis colis = Colis.builder()
                    .codeTracking(codeTracking)
                    .agenceOrigine(agenceOrigine)
                    .agenceRetrait(agenceRetrait)
                    .creePar(utilisateur)
                    .expediteurNom(requete.expediteurNom())
                    .expediteurTelephone(requete.expediteurTelephone())
                    .expediteurEmail(requete.expediteurEmail())
                    .destinataireNom(requete.destinataireNom())
                    .destinataireTelephone(requete.destinataireTelephone())
                    .destinataireEmail(requete.destinataireEmail())
                    .destinataireAdresse(requete.destinataireAdresse())
                    .destinataireVille(requete.destinataireVille())
                    .description(requete.description())
                    .poids(requete.poids())
                    .localId(requete.localId())
                    .build();

            Colis colisSauvegarde = colisRepository.save(colis);

            MiseAJourStatut historique = MiseAJourStatut.builder()
                    .colis(colisSauvegarde)
                    .ancienStatut(null)
                    .statut(StatutColis.ENREGISTRE)
                    .commentaire("Colis synchronise depuis l'application mobile")
                    .utilisateur(utilisateur)
                    .build();
            miseAJourStatutRepository.save(historique);

            quotaService.enregistrerColis(agenceOrigine);

            return new ResultatSyncColisReponse(
                    requete.localId(), colisSauvegarde.getCodeTracking(), true, false, null);

        } catch (Exception e) {
            journal.error("Echec sync colis localId={} : {}", requete.localId(), e.getMessage());
            return new ResultatSyncColisReponse(
                    requete.localId(), null, false, false, e.getMessage());
        }
    }

    private void enregistrerLog(
            Utilisateur utilisateur, int nbEnvoyes, int nbReussis, int nbEchecs,
            LocalDateTime dateDebut) {

        SyncLog log = SyncLog.builder()
                .utilisateur(utilisateur)
                .agence(utilisateur.getAgence())
                .nbColisEnvoyes(nbEnvoyes)
                .nbColisReussis(nbReussis)
                .nbColisEchecs(nbEchecs)
                .dateDebut(dateDebut)
                .dateFin(LocalDateTime.now())
                .build();

        syncLogRepository.save(log);
    }

    private String genererCodeTrackingUnique() {
        for (int tentative = 0; tentative < TENTATIVES_MAX_CODE_TRACKING; tentative++) {
            String code = GenerateurCodeTracking.generer();
            if (colisRepository.findByCodeTrackingAndSupprimeFalse(code).isEmpty()) {
                return code;
            }
        }
        throw new IllegalStateException(
                "Impossible de generer un code tracking unique apres "
                + TENTATIVES_MAX_CODE_TRACKING + " tentatives");
    }
}
