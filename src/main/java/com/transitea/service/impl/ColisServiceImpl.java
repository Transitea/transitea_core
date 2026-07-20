package com.transitea.service.impl;

import com.transitea.dto.request.CreationColisRequete;
import com.transitea.dto.request.MiseAJourStatutRequete;
import com.transitea.dto.response.ColisReponse;
import com.transitea.dto.response.ReponsePagee;
import com.transitea.dto.response.StatistiquesReponse;
import com.transitea.entity.Agence;
import com.transitea.entity.Colis;
import com.transitea.entity.MiseAJourStatut;
import com.transitea.entity.Utilisateur;
import com.transitea.entity.enums.Role;
import com.transitea.entity.enums.StatutColis;
import com.transitea.exception.AccesNonAutoriseException;
import com.transitea.exception.EntiteNonTrouveeException;
import com.transitea.mapper.ColisMapper;
import com.transitea.repository.AgenceRepository;
import com.transitea.repository.ColisRepository;
import com.transitea.repository.MiseAJourStatutRepository;
import com.transitea.service.ColisService;
import com.transitea.service.NotificationService;
import com.transitea.service.QrCodeService;
import com.transitea.service.QuotaService;
import com.transitea.util.GenerateurCodeTracking;
import com.transitea.util.ValidateurTransitionStatut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class ColisServiceImpl implements ColisService {

    private static final Logger journal = LoggerFactory.getLogger(ColisServiceImpl.class);
    private static final int TENTATIVES_MAX_CODE_TRACKING = 5;

    private final ColisRepository colisRepository;
    private final AgenceRepository agenceRepository;
    private final MiseAJourStatutRepository miseAJourStatutRepository;
    private final ColisMapper colisMapper;
    private final QrCodeService qrCodeService;
    private final NotificationService notificationService;
    private final QuotaService quotaService;

    @Value("${application.base-url:http://localhost:8080}")
    private String baseUrl;

    public ColisServiceImpl(
            ColisRepository colisRepository,
            AgenceRepository agenceRepository,
            MiseAJourStatutRepository miseAJourStatutRepository,
            ColisMapper colisMapper,
            QrCodeService qrCodeService,
            NotificationService notificationService,
            QuotaService quotaService) {
        this.colisRepository = colisRepository;
        this.agenceRepository = agenceRepository;
        this.miseAJourStatutRepository = miseAJourStatutRepository;
        this.colisMapper = colisMapper;
        this.qrCodeService = qrCodeService;
        this.notificationService = notificationService;
        this.quotaService = quotaService;
    }

    @Override
    public ColisReponse creer(CreationColisRequete requete, Utilisateur creePar) {
        Agence agenceOrigine = recupererAgenceOuEchouer(requete.agenceOrigineId());
        Agence agenceRetrait = recupererAgenceOuEchouer(requete.agenceRetraitId());

        String codeTracking = genererCodeTrackingUnique();

        Colis colis = Colis.builder()
                .codeTracking(codeTracking)
                .agenceOrigine(agenceOrigine)
                .agenceRetrait(agenceRetrait)
                .creePar(creePar)
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

        enregistrerHistoriqueStatut(
                colisSauvegarde, null, StatutColis.ENREGISTRE, null, null, creePar);

        quotaService.enregistrerColis(agenceOrigine);

        journal.info("Colis cree avec le code : {}", codeTracking);
        return colisMapper.versReponse(colisSauvegarde);
    }

    @Override
    @Transactional(readOnly = true)
    public ReponsePagee<ColisReponse> lister(
            Utilisateur utilisateur, StatutColis statut, Pageable pageable) {

        Page<Colis> page;

        if (utilisateur.getRole() == Role.ADMIN) {
            page = statut != null
                    ? colisRepository.findByStatutActuelAndSupprimeFalse(statut, pageable)
                    : colisRepository.findBySupprimeFalse(pageable);
        } else {
            Agence agence = agenceDeLUtilisateur(utilisateur);
            page = statut != null
                    ? colisRepository.findByAgenceAndStatutActuelAndSupprimeFalse(agence, statut, pageable)
                    : colisRepository.findByAgenceAndSupprimeFalse(agence, pageable);
        }

        return ReponsePagee.depuis(page.map(colisMapper::versReponse));
    }

    @Override
    @Transactional(readOnly = true)
    public ReponsePagee<ColisReponse> rechercher(
            Utilisateur utilisateur, String recherche, Pageable pageable) {

        Page<Colis> page;

        if (utilisateur.getRole() == Role.ADMIN) {
            page = colisRepository.rechercherTous(recherche, pageable);
        } else {
            page = colisRepository.rechercherParAgence(agenceDeLUtilisateur(utilisateur), recherche, pageable);
        }

        return ReponsePagee.depuis(page.map(colisMapper::versReponse));
    }

    @Override
    @Transactional(readOnly = true)
    public ColisReponse trouverParId(Long id, Utilisateur utilisateur) {
        Colis colis = recupererColisOuEchouer(id);
        verifierAcces(colis, utilisateur);

        List<MiseAJourStatut> historique =
                miseAJourStatutRepository.findByColisOrderByDateCreationAsc(colis);

        ColisReponse reponseBase = colisMapper.versReponse(colis);

        return new ColisReponse(
                reponseBase.id(),
                reponseBase.uuid(),
                reponseBase.codeTracking(),
                reponseBase.agenceOrigineId(),
                reponseBase.agenceOrigineNom(),
                reponseBase.agenceRetraitId(),
                reponseBase.agenceRetraitNom(),
                reponseBase.expediteurNom(),
                reponseBase.expediteurTelephone(),
                reponseBase.expediteurEmail(),
                reponseBase.destinataireNom(),
                reponseBase.destinataireTelephone(),
                reponseBase.destinataireEmail(),
                reponseBase.destinataireAdresse(),
                reponseBase.destinataireVille(),
                reponseBase.description(),
                reponseBase.poids(),
                reponseBase.statutActuel(),
                reponseBase.localId(),
                reponseBase.version(),
                reponseBase.dateCreation(),
                colisMapper.versMiseAJourReponses(historique)
        );
    }

    @Override
    public ColisReponse mettreAJourStatut(
            Long id, MiseAJourStatutRequete requete, Utilisateur utilisateur) {

        Colis colis = recupererColisOuEchouer(id);
        verifierAcces(colis, utilisateur);

        StatutColis ancienStatut = colis.getStatutActuel();
        ValidateurTransitionStatut.valider(ancienStatut, requete.statut());

        colis.setStatutActuel(requete.statut());
        Colis colusMisAJour = colisRepository.save(colis);

        enregistrerHistoriqueStatut(
                colusMisAJour,
                ancienStatut,
                requete.statut(),
                requete.localisation(),
                requete.commentaire(),
                utilisateur
        );

        journal.info("Statut du colis {} mis a jour : {} -> {}",
                colis.getCodeTracking(), ancienStatut, requete.statut());

        notificationService.notifierChangementStatut(colusMisAJour, ancienStatut);

        return colisMapper.versReponse(colusMisAJour);
    }

    @Override
    public ColisReponse retirer(String codeTracking, Utilisateur utilisateur) {
        Colis colis = colisRepository.findByCodeTrackingAndSupprimeFalse(codeTracking)
                .orElseThrow(() -> new EntiteNonTrouveeException("Colis", codeTracking));
        verifierAccesRetrait(colis, utilisateur);

        StatutColis ancienStatut = colis.getStatutActuel();
        ValidateurTransitionStatut.valider(ancienStatut, StatutColis.RETIRE);

        colis.setStatutActuel(StatutColis.RETIRE);
        Colis colisRetire = colisRepository.save(colis);

        enregistrerHistoriqueStatut(
                colisRetire, ancienStatut, StatutColis.RETIRE,
                colis.getAgenceRetrait().getNom(),
                "Retrait valide par scan du QR code", utilisateur);

        journal.info("Colis {} retire par scan QR a l'agence {}",
                colis.getCodeTracking(), colis.getAgenceRetrait().getNom());

        notificationService.notifierChangementStatut(colisRetire, ancienStatut);

        return colisMapper.versReponse(colisRetire);
    }

    @Override
    public void supprimer(Long id, Utilisateur utilisateur) {
        Colis colis = recupererColisOuEchouer(id);
        verifierAcces(colis, utilisateur);

        colis.setSupprime(true);
        colisRepository.save(colis);

        journal.info("Colis {} supprime (soft delete)", colis.getCodeTracking());
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] genererQrCode(Long id, Utilisateur utilisateur) {
        Colis colis = recupererColisOuEchouer(id);
        verifierAcces(colis, utilisateur);
        String urlTracking = baseUrl + "/v1/tracking/" + colis.getCodeTracking();
        return qrCodeService.generer(urlTracking, colis.getCodeTracking());
    }

    @Override
    @Transactional(readOnly = true)
    public StatistiquesReponse obtenirStatistiques(Utilisateur utilisateur) {
        Map<StatutColis, Long> parStatut = Arrays.stream(StatutColis.values())
                .collect(Collectors.toMap(
                        statut -> statut,
                        statut -> utilisateur.getRole() == Role.ADMIN
                                ? colisRepository.countByStatutActuelAndSupprimeFalse(statut)
                                : colisRepository.countByAgenceAndStatutActuelAndSupprimeFalse(
                                        agenceDeLUtilisateur(utilisateur), statut)
                ));

        long total = parStatut.values().stream().mapToLong(Long::longValue).sum();
        return new StatistiquesReponse(total, parStatut);
    }

    private Colis recupererColisOuEchouer(Long id) {
        return colisRepository.findById(id)
                .filter(c -> !c.getSupprime())
                .orElseThrow(() -> new EntiteNonTrouveeException("Colis", id));
    }

    private Agence recupererAgenceOuEchouer(Long id) {
        return agenceRepository.findByIdAndSupprimeFalse(id)
                .orElseThrow(() -> new EntiteNonTrouveeException("Agence", id));
    }

    private Agence agenceDeLUtilisateur(Utilisateur utilisateur) {
        if (utilisateur.getAgence() == null) {
            throw new AccesNonAutoriseException();
        }
        return utilisateur.getAgence();
    }

    private void verifierAcces(Colis colis, Utilisateur utilisateur) {
        if (utilisateur.getRole() == Role.ADMIN) {
            return;
        }

        Agence agence = agenceDeLUtilisateur(utilisateur);
        boolean concerneParAgence =
                colis.getAgenceOrigine().getId().equals(agence.getId())
                        || colis.getAgenceRetrait().getId().equals(agence.getId());

        if (!concerneParAgence) {
            throw new AccesNonAutoriseException();
        }
    }

    /**
     * Le retrait ne peut etre valide que par un ADMIN ou par un agent/operateur
     * de l'agence de retrait elle-meme (pas l'agence d'origine).
     */
    private void verifierAccesRetrait(Colis colis, Utilisateur utilisateur) {
        if (utilisateur.getRole() == Role.ADMIN) {
            return;
        }

        Agence agence = agenceDeLUtilisateur(utilisateur);
        if (!colis.getAgenceRetrait().getId().equals(agence.getId())) {
            throw new AccesNonAutoriseException();
        }
    }

    private void enregistrerHistoriqueStatut(
            Colis colis,
            StatutColis ancienStatut,
            StatutColis nouveauStatut,
            String localisation,
            String commentaire,
            Utilisateur utilisateur) {

        MiseAJourStatut historique = MiseAJourStatut.builder()
                .colis(colis)
                .ancienStatut(ancienStatut)
                .statut(nouveauStatut)
                .localisation(localisation)
                .commentaire(commentaire)
                .utilisateur(utilisateur)
                .build();

        miseAJourStatutRepository.save(historique);
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
