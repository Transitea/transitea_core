package com.transitea.service.impl;

import com.transitea.dto.request.CreationColisRequete;
import com.transitea.dto.request.SyncRequete;
import com.transitea.dto.response.ResultatSyncColisReponse;
import com.transitea.dto.response.SyncReponse;
import com.transitea.entity.Colis;
import com.transitea.entity.MiseAJourStatut;
import com.transitea.entity.SyncLog;
import com.transitea.entity.Utilisateur;
import com.transitea.entity.enums.StatutColis;
import com.transitea.repository.ColisRepository;
import com.transitea.repository.MiseAJourStatutRepository;
import com.transitea.repository.SyncLogRepository;
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
    private final MiseAJourStatutRepository miseAJourStatutRepository;
    private final SyncLogRepository syncLogRepository;

    public SyncServiceImpl(
            ColisRepository colisRepository,
            MiseAJourStatutRepository miseAJourStatutRepository,
            SyncLogRepository syncLogRepository) {
        this.colisRepository = colisRepository;
        this.miseAJourStatutRepository = miseAJourStatutRepository;
        this.syncLogRepository = syncLogRepository;
    }

    @Override
    public SyncReponse synchroniser(SyncRequete requete, Utilisateur transporteur) {
        LocalDateTime dateDebut = LocalDateTime.now();
        List<ResultatSyncColisReponse> resultats = new ArrayList<>();
        int nbReussis = 0;
        int nbDoublons = 0;
        int nbEchecs = 0;

        for (CreationColisRequete colisRequete : requete.colis()) {
            ResultatSyncColisReponse resultat = traiterUnColis(colisRequete, transporteur);
            resultats.add(resultat);

            if (resultat.doublon()) {
                nbDoublons++;
            } else if (resultat.succes()) {
                nbReussis++;
            } else {
                nbEchecs++;
            }
        }

        enregistrerLog(transporteur, requete.colis().size(), nbReussis, nbEchecs, dateDebut);

        journal.info("Sync pour {} : {} envoyes, {} reussis, {} doublons, {} echecs",
                transporteur.getEmail(), requete.colis().size(), nbReussis, nbDoublons, nbEchecs);

        return new SyncReponse(requete.colis().size(), nbReussis, nbDoublons, nbEchecs, resultats);
    }

    private ResultatSyncColisReponse traiterUnColis(
            CreationColisRequete requete, Utilisateur transporteur) {

        if (requete.localId() != null) {
            var existant = colisRepository.findByTransporteurAndLocalIdAndSupprimeFalse(
                    transporteur, requete.localId());
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
            String codeTracking = genererCodeTrackingUnique();

            Colis colis = Colis.builder()
                    .codeTracking(codeTracking)
                    .transporteur(transporteur)
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
                    .utilisateur(transporteur)
                    .build();
            miseAJourStatutRepository.save(historique);

            return new ResultatSyncColisReponse(
                    requete.localId(), colisSauvegarde.getCodeTracking(), true, false, null);

        } catch (Exception e) {
            journal.error("Echec sync colis localId={} : {}", requete.localId(), e.getMessage());
            return new ResultatSyncColisReponse(
                    requete.localId(), null, false, false, e.getMessage());
        }
    }

    private void enregistrerLog(
            Utilisateur transporteur, int nbEnvoyes, int nbReussis, int nbEchecs,
            LocalDateTime dateDebut) {

        SyncLog log = SyncLog.builder()
                .transporteur(transporteur)
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
