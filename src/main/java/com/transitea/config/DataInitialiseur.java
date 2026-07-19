package com.transitea.config;

import com.transitea.entity.Agence;
import com.transitea.entity.Colis;
import com.transitea.entity.Enseigne;
import com.transitea.entity.MiseAJourStatut;
import com.transitea.entity.Utilisateur;
import com.transitea.entity.enums.PalierAbonnement;
import com.transitea.entity.enums.Role;
import com.transitea.entity.enums.StatutColis;
import com.transitea.repository.AgenceRepository;
import com.transitea.repository.ColisRepository;
import com.transitea.repository.EnseigneRepository;
import com.transitea.repository.MiseAJourStatutRepository;
import com.transitea.repository.UtilisateurRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
@Profile("dev")
public class DataInitialiseur implements CommandLineRunner {

    private static final Logger journal = LoggerFactory.getLogger(DataInitialiseur.class);

    private final EnseigneRepository enseigneRepository;
    private final AgenceRepository agenceRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final ColisRepository colisRepository;
    private final MiseAJourStatutRepository miseAJourStatutRepository;
    private final PasswordEncoder encodeurMotDePasse;

    public DataInitialiseur(
            EnseigneRepository enseigneRepository,
            AgenceRepository agenceRepository,
            UtilisateurRepository utilisateurRepository,
            ColisRepository colisRepository,
            MiseAJourStatutRepository miseAJourStatutRepository,
            PasswordEncoder encodeurMotDePasse) {
        this.enseigneRepository = enseigneRepository;
        this.agenceRepository = agenceRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.colisRepository = colisRepository;
        this.miseAJourStatutRepository = miseAJourStatutRepository;
        this.encodeurMotDePasse = encodeurMotDePasse;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (utilisateurRepository.count() > 0) {
            journal.info("[DEV] Donnees deja presentes, initialisation ignoree");
            return;
        }

        journal.info("[DEV] Initialisation des donnees de test...");

        Enseigne enseigne = creerEnseigne();
        Agence agenceParis = creerAgence(enseigne, "Agence Paris", "Paris", "12 Rue de Rivoli, 75004 Paris");
        Agence agenceLyon = creerAgence(enseigne, "Agence Lyon", "Lyon", "8 Rue de la Republique, 69002 Lyon");
        Agence agenceMarseille = creerAgence(enseigne, "Agence Marseille", "Marseille", "45 La Canebiere, 13001 Marseille");

        Utilisateur admin = creerAdmin();
        creerOperateur(agenceParis);
        Utilisateur agent = creerAgent(agenceParis);

        creerColisAvecHistorique(agenceParis, agenceLyon, agenceMarseille, agent, admin);

        journal.info("[DEV] Donnees de test inserees avec succes");
        journal.info("[DEV] --- Comptes disponibles ---");
        journal.info("[DEV] ADMIN     : admin@transitea.fr     / admin123");
        journal.info("[DEV] OPERATEUR : operateur@transitea.fr / operateur123");
        journal.info("[DEV] AGENT     : agent@transitea.fr     / agent123");
    }

    private Enseigne creerEnseigne() {
        Enseigne enseigne = Enseigne.builder()
                .nom("Transitea France")
                .palierAbonnement(PalierAbonnement.STANDARD)
                .quotaColisMois(5000)
                .dateDebutAbonnement(LocalDateTime.now())
                .build();
        return enseigneRepository.save(enseigne);
    }

    private Agence creerAgence(Enseigne enseigne, String nom, String ville, String adresse) {
        Agence agence = Agence.builder()
                .nom(nom)
                .ville(ville)
                .adresse(adresse)
                .enseigne(enseigne)
                .build();
        return agenceRepository.save(agence);
    }

    private Utilisateur creerAdmin() {
        Utilisateur admin = Utilisateur.builder()
                .nom("Moreau")
                .prenom("Camille")
                .email("admin@transitea.fr")
                .telephone("+33612345678")
                .motDePasseHash(encodeurMotDePasse.encode("admin123"))
                .role(Role.ADMIN)
                .build();
        return utilisateurRepository.save(admin);
    }

    private Utilisateur creerOperateur(Agence agence) {
        Utilisateur operateur = Utilisateur.builder()
                .nom("Bernard")
                .prenom("Julien")
                .email("operateur@transitea.fr")
                .telephone("+33623456789")
                .motDePasseHash(encodeurMotDePasse.encode("operateur123"))
                .role(Role.OPERATEUR)
                .agence(agence)
                .build();
        return utilisateurRepository.save(operateur);
    }

    private Utilisateur creerAgent(Agence agence) {
        Utilisateur agent = Utilisateur.builder()
                .nom("Lefebvre")
                .prenom("Manon")
                .email("agent@transitea.fr")
                .telephone("+33634567890")
                .motDePasseHash(encodeurMotDePasse.encode("agent123"))
                .role(Role.AGENT)
                .agence(agence)
                .build();
        return utilisateurRepository.save(agent);
    }

    private void creerColisAvecHistorique(
            Agence agenceParis, Agence agenceLyon, Agence agenceMarseille,
            Utilisateur agent, Utilisateur admin) {

        Colis colisEnregistre = creerColis(
                agenceParis, agenceLyon, agent, "TRA-2026-TEST01",
                "Thomas Girard", "+33645678901", "thomas.girard@exemple.fr",
                "Alice Dubois", "+33656789012", "alice.dubois@exemple.fr",
                "5 Rue Victor Hugo, 69003 Lyon", "Lyon",
                "Vetements", new BigDecimal("3.500"),
                StatutColis.ENREGISTRE
        );
        enregistrerHistorique(colisEnregistre, null, StatutColis.ENREGISTRE,
                "Agence Paris", "Colis enregistre a la reception", agent);

        Colis colisEnTransit = creerColis(
                agenceParis, agenceMarseille, agent, "TRA-2026-TEST02",
                "Sophie Petit", "+33667890123", null,
                "Nicolas Roux", "+33678901234", "nicolas.roux@exemple.fr",
                "22 Rue Paradis, 13006 Marseille", "Marseille",
                "Materiel informatique - fragile", new BigDecimal("12.000"),
                StatutColis.EN_TRANSIT
        );
        enregistrerHistorique(colisEnTransit, null, StatutColis.ENREGISTRE,
                "Agence Paris", null, agent);
        enregistrerHistorique(colisEnTransit, StatutColis.ENREGISTRE, StatutColis.EN_TRANSIT,
                "Autoroute A6", "En route vers Marseille", admin);

        Colis colisRetire = creerColis(
                agenceParis, agenceParis, agent, "TRA-2026-TEST03",
                "Claire Fontaine", "+33689012345", "claire.fontaine@exemple.fr",
                "Hugo Mercier", "+33690123456", "hugo.mercier@exemple.fr",
                "3 Avenue des Champs-Elysees, 75008 Paris", "Paris",
                "Documents", new BigDecimal("1.200"),
                StatutColis.RETIRE
        );
        enregistrerHistorique(colisRetire, null, StatutColis.ENREGISTRE,
                "Agence Paris", null, agent);
        enregistrerHistorique(colisRetire, StatutColis.ENREGISTRE, StatutColis.EN_TRANSIT,
                "Agence Paris", null, admin);
        enregistrerHistorique(colisRetire, StatutColis.EN_TRANSIT, StatutColis.ARRIVE_AGENCE,
                "Agence Paris", "Arrive a l'agence de retrait", admin);
        enregistrerHistorique(colisRetire, StatutColis.ARRIVE_AGENCE, StatutColis.RETIRE,
                "Agence Paris", "Retire et signe par le destinataire", agent);

        Colis colisArriveAgence = creerColis(
                agenceParis, agenceLyon, agent, "TRA-2026-TEST04",
                "Laurent Simon", "+33601234567", null,
                "Emma Michel", "+33612340987", null,
                "17 Rue Garibaldi, 69006 Lyon", "Lyon",
                "Equipement electronique", new BigDecimal("8.750"),
                StatutColis.ARRIVE_AGENCE
        );
        enregistrerHistorique(colisArriveAgence, null, StatutColis.ENREGISTRE,
                "Agence Paris", null, agent);
        enregistrerHistorique(colisArriveAgence, StatutColis.ENREGISTRE, StatutColis.EN_TRANSIT,
                "Autoroute A6", null, admin);
        enregistrerHistorique(colisArriveAgence, StatutColis.EN_TRANSIT, StatutColis.ARRIVE_AGENCE,
                "Agence Lyon", "Disponible pour retrait", admin);
    }

    private Colis creerColis(
            Agence agenceOrigine, Agence agenceRetrait, Utilisateur creePar, String codeTracking,
            String expediteurNom, String expediteurTel, String expediteurEmail,
            String destinataireNom, String destinataireTel, String destinataireEmail,
            String destinataireAdresse, String destinataireVille,
            String description, BigDecimal poids, StatutColis statut) {

        Colis colis = Colis.builder()
                .codeTracking(codeTracking)
                .agenceOrigine(agenceOrigine)
                .agenceRetrait(agenceRetrait)
                .creePar(creePar)
                .expediteurNom(expediteurNom)
                .expediteurTelephone(expediteurTel)
                .expediteurEmail(expediteurEmail)
                .destinataireNom(destinataireNom)
                .destinataireTelephone(destinataireTel)
                .destinataireEmail(destinataireEmail)
                .destinataireAdresse(destinataireAdresse)
                .destinataireVille(destinataireVille)
                .description(description)
                .poids(poids)
                .statutActuel(statut)
                .build();

        return colisRepository.save(colis);
    }

    private void enregistrerHistorique(
            Colis colis, StatutColis ancienStatut, StatutColis nouveauStatut,
            String localisation, String commentaire, Utilisateur utilisateur) {

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
}
