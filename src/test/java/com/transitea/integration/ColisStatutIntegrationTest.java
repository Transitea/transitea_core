package com.transitea.integration;

import com.transitea.dto.request.CreationColisRequete;
import com.transitea.dto.request.MiseAJourStatutRequete;
import com.transitea.dto.response.ColisReponse;
import com.transitea.entity.Agence;
import com.transitea.entity.Enseigne;
import com.transitea.entity.Utilisateur;
import com.transitea.entity.enums.PalierAbonnement;
import com.transitea.entity.enums.Role;
import com.transitea.entity.enums.StatutColis;
import com.transitea.repository.AgenceRepository;
import com.transitea.repository.ColisRepository;
import com.transitea.repository.EnseigneRepository;
import com.transitea.repository.UtilisateurRepository;
import com.transitea.service.ColisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Verifie de bout en bout (vrai contexte Spring, vraie persistence H2/Hibernate,
 * pas de mock) que le nouveau statut EN_COURS_DE_LIVRAISON peut etre enregistre
 * et relu sans erreur - a la difference des tests unitaires de ColisServiceImplTest
 * qui mockent le repository et ne peuvent pas detecter un probleme de mapping JPA.
 *
 * Pas de @Transactional ici : notifierChangementStatut tourne en @Async sur un
 * autre thread/connexion, qui ne verrait pas un colis insere dans une transaction
 * de test non commitee (FOREIGN KEY violation en H2). Chaque test genere ses
 * propres donnees (code tracking aleatoire) pour ne pas interferer entre eux.
 */
@SpringBootTest
@ActiveProfiles("test")
class ColisStatutIntegrationTest {

    @Autowired
    private ColisService colisService;

    @Autowired
    private EnseigneRepository enseigneRepository;

    @Autowired
    private AgenceRepository agenceRepository;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private ColisRepository colisRepository;

    private Utilisateur agent;
    private Agence agenceOrigine;
    private Agence agenceRetrait;

    @BeforeEach
    void initialiser() {
        Enseigne enseigne = enseigneRepository.save(Enseigne.builder()
                .nom("Transitea Test")
                .palierAbonnement(PalierAbonnement.STANDARD)
                .quotaColisMois(5000)
                .dateDebutAbonnement(LocalDateTime.now())
                .build());

        agenceOrigine = agenceRepository.save(Agence.builder()
                .nom("Agence Origine")
                .ville("Kinshasa")
                .adresse("1 Avenue Test")
                .enseigne(enseigne)
                .build());

        agenceRetrait = agenceRepository.save(Agence.builder()
                .nom("Agence Retrait")
                .ville("Goma")
                .adresse("2 Avenue Test")
                .enseigne(enseigne)
                .build());

        agent = utilisateurRepository.save(Utilisateur.builder()
                .nom("Test")
                .prenom("Agent")
                .email("agent-integration-" + java.util.UUID.randomUUID() + "@transitea.test")
                .telephone("+2439" + System.nanoTime() % 100_000_000L)
                .motDePasseHash("hash")
                .role(Role.AGENT)
                .agence(agenceOrigine)
                .build());
    }

    @Test
    void doit_creer_et_faire_transiter_un_colis_vers_en_cours_de_livraison_sans_erreur() {
        CreationColisRequete creation = new CreationColisRequete(
                agenceOrigine.getId(), agenceRetrait.getId(),
                "Jean Expediteur", "+243900000001", "jean@example.com",
                "Marie Destinataire", "+243900000002", "marie@example.com",
                "10 Avenue de la Livraison", "Kinshasa",
                "Colis test", new BigDecimal("1.000"), null
        );

        ColisReponse cree = colisService.creer(creation, agent);
        assertThat(cree.statutActuel()).isEqualTo(StatutColis.ENREGISTRE);

        assertThatCode(() -> colisService.mettreAJourStatut(
                cree.id(),
                new MiseAJourStatutRequete(StatutColis.EN_COURS_DE_LIVRAISON, "Kinshasa", "Livraison directe"),
                agent))
                .doesNotThrowAnyException();

        StatutColis persiste = colisRepository.findById(cree.id())
                .orElseThrow()
                .getStatutActuel();
        assertThat(persiste).isEqualTo(StatutColis.EN_COURS_DE_LIVRAISON);
    }

    @Test
    void doit_faire_transiter_en_cours_de_livraison_puis_retire_sans_erreur() {
        CreationColisRequete creation = new CreationColisRequete(
                agenceOrigine.getId(), agenceRetrait.getId(),
                "Jean Expediteur", null, null,
                "Marie Destinataire", null, null,
                "10 Avenue de la Livraison", "Kinshasa",
                "Colis test", new BigDecimal("1.000"), null
        );
        ColisReponse cree = colisService.creer(creation, agent);

        colisService.mettreAJourStatut(
                cree.id(),
                new MiseAJourStatutRequete(StatutColis.EN_COURS_DE_LIVRAISON, null, null),
                agent);

        assertThatCode(() -> colisService.mettreAJourStatut(
                cree.id(),
                new MiseAJourStatutRequete(StatutColis.RETIRE, null, "Remis en mains propres"),
                agent))
                .doesNotThrowAnyException();

        assertThat(colisRepository.findById(cree.id()).orElseThrow().getStatutActuel())
                .isEqualTo(StatutColis.RETIRE);
    }
}
