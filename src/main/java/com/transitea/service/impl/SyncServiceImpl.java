package com.transitea.service.impl;

import com.transitea.dto.request.CreationColisRequete;
import com.transitea.dto.request.MiseAJourStatutSyncItem;
import com.transitea.dto.request.SyncUploadRequete;
import com.transitea.dto.response.ColisReponse;
import com.transitea.dto.response.ResultatSyncColisReponse;
import com.transitea.dto.response.ResultatSyncStatutReponse;
import com.transitea.dto.response.SyncDownloadReponse;
import com.transitea.dto.response.SyncUploadReponse;
import com.transitea.entity.Agence;
import com.transitea.entity.Colis;
import com.transitea.entity.MiseAJourStatut;
import com.transitea.entity.SyncLog;
import com.transitea.entity.Utilisateur;
import com.transitea.entity.enums.Role;
import com.transitea.entity.enums.StatutColis;
import com.transitea.exception.AccesNonAutoriseException;
import com.transitea.exception.EntiteNonTrouveeException;
import com.transitea.mapper.ColisMapper;
import com.transitea.repository.AgenceRepository;
import com.transitea.repository.ColisRepository;
import com.transitea.repository.MiseAJourStatutRepository;
import com.transitea.repository.SyncLogRepository;
import com.transitea.service.NotificationService;
import com.transitea.service.QuotaService;
import com.transitea.service.SyncService;
import com.transitea.util.GenerateurCodeTracking;
import com.transitea.util.ValidateurTransitionStatut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
    private final NotificationService notificationService;
    private final ColisMapper colisMapper;

    public SyncServiceImpl(
            ColisRepository colisRepository,
            AgenceRepository agenceRepository,
            MiseAJourStatutRepository miseAJourStatutRepository,
            SyncLogRepository syncLogRepository,
            QuotaService quotaService,
            NotificationService notificationService,
            ColisMapper colisMapper) {
        this.colisRepository = colisRepository;
        this.agenceRepository = agenceRepository;
        this.miseAJourStatutRepository = miseAJourStatutRepository;
        this.syncLogRepository = syncLogRepository;
        this.quotaService = quotaService;
        this.notificationService = notificationService;
        this.colisMapper = colisMapper;
    }

    @Override
    public SyncUploadReponse synchroniserUpload(SyncUploadRequete requete, Utilisateur utilisateur) {
        LocalDateTime dateDebut = LocalDateTime.now();

        List<ResultatSyncColisReponse> resultatsColis = new ArrayList<>();
        int nbColisReussis = 0;
        int nbColisDoublons = 0;
        int nbColisEchecs = 0;

        for (CreationColisRequete colisRequete : requete.colis()) {
            ResultatSyncColisReponse resultat = traiterUnColis(colisRequete, utilisateur);
            resultatsColis.add(resultat);

            if (resultat.doublon()) {
                nbColisDoublons++;
            } else if (resultat.succes()) {
                nbColisReussis++;
            } else {
                nbColisEchecs++;
            }
        }

        List<ResultatSyncStatutReponse> resultatsStatuts = new ArrayList<>();
        int nbStatutsReussis = 0;
        int nbStatutsConflits = 0;
        int nbStatutsEchecs = 0;

        for (MiseAJourStatutSyncItem item : requete.misesAJourStatut()) {
            ResultatSyncStatutReponse resultat = traiterUneMiseAJourStatut(item, utilisateur);
            resultatsStatuts.add(resultat);

            if (resultat.succes()) {
                nbStatutsReussis++;
            } else if (resultat.conflit()) {
                nbStatutsConflits++;
            } else {
                nbStatutsEchecs++;
            }
        }

        enregistrerLog(utilisateur, requete.colis().size(), nbColisReussis, nbColisEchecs, dateDebut);

        journal.info(
                "Sync upload pour {} : {} colis envoyes ({} reussis, {} doublons, {} echecs), "
                + "{} statuts envoyes ({} reussis, {} conflits, {} echecs)",
                utilisateur.getEmail(),
                requete.colis().size(), nbColisReussis, nbColisDoublons, nbColisEchecs,
                requete.misesAJourStatut().size(), nbStatutsReussis, nbStatutsConflits, nbStatutsEchecs);

        return new SyncUploadReponse(
                requete.colis().size(), nbColisReussis, nbColisDoublons, nbColisEchecs, resultatsColis,
                requete.misesAJourStatut().size(), nbStatutsReussis, nbStatutsConflits, nbStatutsEchecs,
                resultatsStatuts);
    }

    @Override
    @Transactional(readOnly = true)
    public SyncDownloadReponse synchroniserDownload(LocalDateTime depuis, Utilisateur utilisateur) {
        List<Colis> colisModifies = utilisateur.getRole() == Role.ADMIN
                ? colisRepository.trouverModifiesDepuis(depuis)
                : colisRepository.trouverModifiesDepuisParAgence(agenceDeLUtilisateur(utilisateur), depuis);

        LocalDateTime curseur = LocalDateTime.now();
        List<ColisReponse> reponses = colisMapper.versReponses(colisModifies);

        journal.info("Sync download pour {} depuis {} : {} colis renvoyes",
                utilisateur.getEmail(), depuis, reponses.size());

        return new SyncDownloadReponse(reponses, curseur);
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

    private ResultatSyncStatutReponse traiterUneMiseAJourStatut(
            MiseAJourStatutSyncItem item, Utilisateur utilisateur) {

        try {
            Colis colis = resoudreColis(item, utilisateur);
            verifierAccesMiseAJour(colis, item.nouveauStatut(), utilisateur);

            if (item.baseVersion() != null && !item.baseVersion().equals(colis.getVersion())) {
                boolean clientPlusRecent = estClientPlusRecent(colis, item.dateChangementClient());
                if (!clientPlusRecent) {
                    journal.debug("Conflit de version pour le colis {} (localId={}) : "
                                  + "modification serveur plus recente, changement client ignore",
                            colis.getCodeTracking(), item.localId());
                    return new ResultatSyncStatutReponse(
                            colis.getId(), item.localId(), colis.getCodeTracking(),
                            false, true, colis.getStatutActuel(),
                            "Conflit de version : une modification plus recente existe deja sur le serveur");
                }
            }

            StatutColis ancienStatut = colis.getStatutActuel();
            ValidateurTransitionStatut.valider(ancienStatut, item.nouveauStatut());

            colis.setStatutActuel(item.nouveauStatut());
            Colis colisMisAJour = colisRepository.save(colis);

            MiseAJourStatut historique = MiseAJourStatut.builder()
                    .colis(colisMisAJour)
                    .ancienStatut(ancienStatut)
                    .statut(item.nouveauStatut())
                    .commentaire(item.commentaire())
                    .utilisateur(utilisateur)
                    .build();
            miseAJourStatutRepository.save(historique);

            notificationService.notifierChangementStatut(colisMisAJour, ancienStatut);

            return new ResultatSyncStatutReponse(
                    colisMisAJour.getId(), item.localId(), colisMisAJour.getCodeTracking(),
                    true, false, item.nouveauStatut(), null);

        } catch (Exception e) {
            journal.error("Echec sync statut colisId={} localId={} : {}",
                    item.colisId(), item.localId(), e.getMessage());
            return new ResultatSyncStatutReponse(
                    item.colisId(), item.localId(), null, false, false, null, e.getMessage());
        }
    }

    private Colis resoudreColis(MiseAJourStatutSyncItem item, Utilisateur utilisateur) {
        if (item.colisId() != null) {
            return colisRepository.findById(item.colisId())
                    .filter(c -> !c.getSupprime())
                    .orElseThrow(() -> new EntiteNonTrouveeException("Colis", item.colisId()));
        }

        if (item.localId() != null) {
            return colisRepository.findByCreeParAndLocalIdAndSupprimeFalse(utilisateur, item.localId())
                    .orElseThrow(() -> new EntiteNonTrouveeException("Colis (localId)", item.localId()));
        }

        throw new EntiteNonTrouveeException("Colis", "colisId ou localId manquant");
    }

    /**
     * Compare le changement en attente cote client au dernier changement connu du
     * serveur (Last-Write-Wins, CDC 7.3) : le changement le plus recent l'emporte.
     */
    private boolean estClientPlusRecent(Colis colis, LocalDateTime dateChangementClient) {
        Optional<MiseAJourStatut> dernierChangementServeur =
                miseAJourStatutRepository.findByColisOrderByDateCreationDesc(colis)
                        .stream()
                        .findFirst();

        if (dernierChangementServeur.isEmpty()) {
            return true;
        }

        return dateChangementClient.isAfter(dernierChangementServeur.get().getDateCreation());
    }

    /**
     * Le retrait ne peut etre valide que par un ADMIN ou l'agence de retrait ;
     * les autres transitions sont ouvertes a l'ADMIN et aux agences concernees
     * (origine ou retrait), a l'image de ColisServiceImpl.
     */
    private void verifierAccesMiseAJour(Colis colis, StatutColis nouveauStatut, Utilisateur utilisateur) {
        if (utilisateur.getRole() == Role.ADMIN) {
            return;
        }

        Agence agence = agenceDeLUtilisateur(utilisateur);

        if (nouveauStatut == StatutColis.RETIRE) {
            if (!colis.getAgenceRetrait().getId().equals(agence.getId())) {
                throw new AccesNonAutoriseException();
            }
            return;
        }

        boolean concerneParAgence =
                colis.getAgenceOrigine().getId().equals(agence.getId())
                        || colis.getAgenceRetrait().getId().equals(agence.getId());

        if (!concerneParAgence) {
            throw new AccesNonAutoriseException();
        }
    }

    private Agence agenceDeLUtilisateur(Utilisateur utilisateur) {
        if (utilisateur.getAgence() == null) {
            throw new AccesNonAutoriseException();
        }
        return utilisateur.getAgence();
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
