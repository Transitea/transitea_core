package com.transitea.config;

import com.transitea.entity.Colis;
import com.transitea.entity.MiseAJourStatut;
import com.transitea.entity.Utilisateur;
import com.transitea.entity.enums.Role;
import com.transitea.entity.enums.StatutColis;
import com.transitea.repository.ColisRepository;
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

@Component
@Profile("dev")
public class DataInitialiseur implements CommandLineRunner {

    private static final Logger journal = LoggerFactory.getLogger(DataInitialiseur.class);

    private final UtilisateurRepository utilisateurRepository;
    private final ColisRepository colisRepository;
    private final MiseAJourStatutRepository miseAJourStatutRepository;
    private final PasswordEncoder encodeurMotDePasse;

    public DataInitialiseur(
            UtilisateurRepository utilisateurRepository,
            ColisRepository colisRepository,
            MiseAJourStatutRepository miseAJourStatutRepository,
            PasswordEncoder encodeurMotDePasse) {
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

        Utilisateur admin       = creerAdmin();
        Utilisateur transporteur = creerTransporteur();
        Utilisateur operateur   = creerOperateur();

        creerColisAvecHistorique(transporteur, admin);

        journal.info("[DEV] Donnees de test inserees avec succes");
        journal.info("[DEV] --- Comptes disponibles ---");
        journal.info("[DEV] ADMIN        : admin@transitea.cd     / admin123");
        journal.info("[DEV] TRANSPORTEUR : transport@transitea.cd / transport123");
        journal.info("[DEV] OPERATEUR    : operateur@transitea.cd / operateur123");
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

    private Utilisateur creerTransporteur() {
        Utilisateur transporteur = Utilisateur.builder()
                .nom("Lumbu")
                .prenom("Louange")
                .email("transport@transitea.cd")
                .telephone("+243900000001")
                .motDePasseHash(encodeurMotDePasse.encode("transport123"))
                .role(Role.TRANSPORTEUR)
                .build();
        return utilisateurRepository.save(transporteur);
    }

    private Utilisateur creerOperateur() {
        Utilisateur operateur = Utilisateur.builder()
                .nom("Mutombo")
                .prenom("Pierre")
                .email("operateur@transitea.cd")
                .telephone("+243900000002")
                .motDePasseHash(encodeurMotDePasse.encode("operateur123"))
                .role(Role.OPERATEUR)
                .build();
        return utilisateurRepository.save(operateur);
    }

    private void creerColisAvecHistorique(Utilisateur transporteur, Utilisateur admin) {
        Colis colisEnregistre = creerColis(
                transporteur, "TRA-2026-TEST01",
                "Kabila Marcel", "+243900000010", "marcel@exemple.cd",
                "Tshisekedi Alain", "+243900000011", "alain@exemple.cd",
                "Avenue Kasa-Vubu N12", "Lubumbashi",
                "Vetements", new BigDecimal("3.500"),
                StatutColis.ENREGISTRE
        );
        enregistrerHistorique(colisEnregistre, null, StatutColis.ENREGISTRE,
                "Depot Kinshasa", "Colis enregistre a la reception", transporteur);

        Colis colisEnTransit = creerColis(
                transporteur, "TRA-2026-TEST02",
                "Nzinga Sophie", "+243900000012", null,
                "Lumumba Robert", "+243900000013", "robert@exemple.cd",
                "Quartier Makutano", "Goma",
                "Materiel informatique - fragile", new BigDecimal("12.000"),
                StatutColis.EN_TRANSIT
        );
        enregistrerHistorique(colisEnTransit, null, StatutColis.ENREGISTRE,
                "Depot Kinshasa", null, transporteur);
        enregistrerHistorique(colisEnTransit, StatutColis.ENREGISTRE, StatutColis.PRIS_EN_CHARGE,
                "Depot Kinshasa", "Pris en charge par le chauffeur", transporteur);
        enregistrerHistorique(colisEnTransit, StatutColis.PRIS_EN_CHARGE, StatutColis.EN_TRANSIT,
                "Route Nationale N1", "En route vers Goma", admin);

        Colis colisLivre = creerColis(
                transporteur, "TRA-2026-TEST03",
                "Kasongo Paul", "+243900000014", "paul@exemple.cd",
                "Mbeki Fatou", "+243900000015", "fatou@exemple.cd",
                "Avenue du Commerce 5", "Kinshasa",
                "Medicaments", new BigDecimal("1.200"),
                StatutColis.LIVRE
        );
        enregistrerHistorique(colisLivre, null, StatutColis.ENREGISTRE,
                "Depot Gombe", null, transporteur);
        enregistrerHistorique(colisLivre, StatutColis.ENREGISTRE, StatutColis.PRIS_EN_CHARGE,
                "Depot Gombe", null, transporteur);
        enregistrerHistorique(colisLivre, StatutColis.PRIS_EN_CHARGE, StatutColis.EN_TRANSIT,
                "Kinshasa centre", null, admin);
        enregistrerHistorique(colisLivre, StatutColis.EN_TRANSIT, StatutColis.ARRIVE_DEPOT,
                "Depot Gombe", "Arrive au depot de destination", admin);
        enregistrerHistorique(colisLivre, StatutColis.ARRIVE_DEPOT, StatutColis.EN_LIVRAISON,
                "Kinshasa", null, transporteur);
        enregistrerHistorique(colisLivre, StatutColis.EN_LIVRAISON, StatutColis.LIVRE,
                "Avenue du Commerce 5", "Livre et signe par le destinataire", transporteur);

        Colis colisEnDouane = creerColis(
                transporteur, "TRA-2026-TEST04",
                "Diallo Ibrahim", "+243900000016", null,
                "Kone Mariam", "+243900000017", null,
                "Cite Verte", "Bukavu",
                "Equipement electronique", new BigDecimal("8.750"),
                StatutColis.EN_DOUANE
        );
        enregistrerHistorique(colisEnDouane, null, StatutColis.ENREGISTRE,
                "Depot Kinshasa", null, transporteur);
        enregistrerHistorique(colisEnDouane, StatutColis.ENREGISTRE, StatutColis.PRIS_EN_CHARGE,
                "Depot Kinshasa", null, transporteur);
        enregistrerHistorique(colisEnDouane, StatutColis.PRIS_EN_CHARGE, StatutColis.EN_TRANSIT,
                "Route vers Bukavu", null, admin);
        enregistrerHistorique(colisEnDouane, StatutColis.EN_TRANSIT, StatutColis.EN_DOUANE,
                "Frontiere Ruzizi", "Verification douaniere en cours", admin);
    }

    private Colis creerColis(
            Utilisateur transporteur, String codeTracking,
            String expediteurNom, String expediteurTel, String expediteurEmail,
            String destinataireNom, String destinataireTel, String destinataireEmail,
            String destinataireAdresse, String destinataireVille,
            String description, BigDecimal poids, StatutColis statut) {

        Colis colis = Colis.builder()
                .codeTracking(codeTracking)
                .transporteur(transporteur)
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
