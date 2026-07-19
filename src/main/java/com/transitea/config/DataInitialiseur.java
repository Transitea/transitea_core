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
        Agence agenceKinshasa = creerAgence(enseigne, "Agence Kinshasa", "Kinshasa", "Avenue Kasa-Vubu N12");
        Agence agenceLubumbashi = creerAgence(enseigne, "Agence Lubumbashi", "Lubumbashi", "Avenue Mobutu N3");
        Agence agenceGoma = creerAgence(enseigne, "Agence Goma", "Goma", "Quartier Makutano");

        Utilisateur admin = creerAdmin();
        creerOperateur(agenceKinshasa);
        Utilisateur agent = creerAgent(agenceKinshasa);

        creerColisAvecHistorique(agenceKinshasa, agenceLubumbashi, agenceGoma, agent, admin);

        journal.info("[DEV] Donnees de test inserees avec succes");
        journal.info("[DEV] --- Comptes disponibles ---");
        journal.info("[DEV] ADMIN     : admin@transitea.cd     / admin123");
        journal.info("[DEV] OPERATEUR : operateur@transitea.cd / operateur123");
        journal.info("[DEV] AGENT     : agent@transitea.cd      / agent123");
    }

    private Enseigne creerEnseigne() {
        Enseigne enseigne = Enseigne.builder()
                .nom("Transitea RDC")
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
                .nom("Lalande")
                .prenom("Jean-Paul")
                .email("admin@transitea.cd")
                .telephone("+243900000000")
                .motDePasseHash(encodeurMotDePasse.encode("admin123"))
                .role(Role.ADMIN)
                .build();
        return utilisateurRepository.save(admin);
    }

    private Utilisateur creerOperateur(Agence agence) {
        Utilisateur operateur = Utilisateur.builder()
                .nom("Mutombo")
                .prenom("Pierre")
                .email("operateur@transitea.cd")
                .telephone("+243900000002")
                .motDePasseHash(encodeurMotDePasse.encode("operateur123"))
                .role(Role.OPERATEUR)
                .agence(agence)
                .build();
        return utilisateurRepository.save(operateur);
    }

    private Utilisateur creerAgent(Agence agence) {
        Utilisateur agent = Utilisateur.builder()
                .nom("Lumbu")
                .prenom("Louange")
                .email("agent@transitea.cd")
                .telephone("+243900000001")
                .motDePasseHash(encodeurMotDePasse.encode("agent123"))
                .role(Role.AGENT)
                .agence(agence)
                .build();
        return utilisateurRepository.save(agent);
    }

    private void creerColisAvecHistorique(
            Agence agenceKinshasa, Agence agenceLubumbashi, Agence agenceGoma,
            Utilisateur agent, Utilisateur admin) {

        Colis colisEnregistre = creerColis(
                agenceKinshasa, agenceLubumbashi, agent, "TRA-2026-TEST01",
                "Kabila Marcel", "+243900000010", "marcel@exemple.cd",
                "Tshisekedi Alain", "+243900000011", "alain@exemple.cd",
                "Avenue Kasa-Vubu N12", "Lubumbashi",
                "Vetements", new BigDecimal("3.500"),
                StatutColis.ENREGISTRE
        );
        enregistrerHistorique(colisEnregistre, null, StatutColis.ENREGISTRE,
                "Agence Kinshasa", "Colis enregistre a la reception", agent);

        Colis colisEnTransit = creerColis(
                agenceKinshasa, agenceGoma, agent, "TRA-2026-TEST02",
                "Nzinga Sophie", "+243900000012", null,
                "Lumumba Robert", "+243900000013", "robert@exemple.cd",
                "Quartier Makutano", "Goma",
                "Materiel informatique - fragile", new BigDecimal("12.000"),
                StatutColis.EN_TRANSIT
        );
        enregistrerHistorique(colisEnTransit, null, StatutColis.ENREGISTRE,
                "Agence Kinshasa", null, agent);
        enregistrerHistorique(colisEnTransit, StatutColis.ENREGISTRE, StatutColis.EN_TRANSIT,
                "Route Nationale N1", "En route vers Goma", admin);

        Colis colisRetire = creerColis(
                agenceKinshasa, agenceKinshasa, agent, "TRA-2026-TEST03",
                "Kasongo Paul", "+243900000014", "paul@exemple.cd",
                "Mbeki Fatou", "+243900000015", "fatou@exemple.cd",
                "Avenue du Commerce 5", "Kinshasa",
                "Medicaments", new BigDecimal("1.200"),
                StatutColis.RETIRE
        );
        enregistrerHistorique(colisRetire, null, StatutColis.ENREGISTRE,
                "Agence Kinshasa", null, agent);
        enregistrerHistorique(colisRetire, StatutColis.ENREGISTRE, StatutColis.EN_TRANSIT,
                "Agence Kinshasa", null, admin);
        enregistrerHistorique(colisRetire, StatutColis.EN_TRANSIT, StatutColis.ARRIVE_AGENCE,
                "Agence Kinshasa", "Arrive a l'agence de retrait", admin);
        enregistrerHistorique(colisRetire, StatutColis.ARRIVE_AGENCE, StatutColis.RETIRE,
                "Agence Kinshasa", "Retire et signe par le destinataire", agent);

        Colis colisArriveAgence = creerColis(
                agenceKinshasa, agenceLubumbashi, agent, "TRA-2026-TEST04",
                "Diallo Ibrahim", "+243900000016", null,
                "Kone Mariam", "+243900000017", null,
                "Cite Verte", "Lubumbashi",
                "Equipement electronique", new BigDecimal("8.750"),
                StatutColis.ARRIVE_AGENCE
        );
        enregistrerHistorique(colisArriveAgence, null, StatutColis.ENREGISTRE,
                "Agence Kinshasa", null, agent);
        enregistrerHistorique(colisArriveAgence, StatutColis.ENREGISTRE, StatutColis.EN_TRANSIT,
                "Route vers Lubumbashi", null, admin);
        enregistrerHistorique(colisArriveAgence, StatutColis.EN_TRANSIT, StatutColis.ARRIVE_AGENCE,
                "Agence Lubumbashi", "Disponible pour retrait", admin);
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
