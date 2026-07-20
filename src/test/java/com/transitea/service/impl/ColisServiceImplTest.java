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
import com.transitea.exception.TransitionStatutInvalideException;
import com.transitea.mapper.ColisMapper;
import com.transitea.repository.AgenceRepository;
import com.transitea.repository.ColisRepository;
import com.transitea.repository.MiseAJourStatutRepository;
import com.transitea.service.NotificationService;
import com.transitea.service.QrCodeService;
import com.transitea.service.QuotaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ColisServiceImplTest {

    @Mock
    private ColisRepository colisRepository;

    @Mock
    private AgenceRepository agenceRepository;

    @Mock
    private MiseAJourStatutRepository miseAJourStatutRepository;

    @Mock
    private ColisMapper colisMapper;

    @Mock
    private QrCodeService qrCodeService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private QuotaService quotaService;

    @InjectMocks
    private ColisServiceImpl colisService;

    private Agence agenceKinshasa;
    private Agence agenceGoma;
    private Agence agenceLubumbashi;
    private Utilisateur agent;
    private Utilisateur autreAgent;
    private Utilisateur agentAgenceRetrait;
    private Utilisateur admin;
    private Utilisateur operateur;
    private Colis colis;
    private ColisReponse colisReponse;

    @BeforeEach
    void initialiser() {
        ReflectionTestUtils.setField(colisService, "baseUrl", "http://localhost:8080");

        agenceKinshasa = Agence.builder().nom("Agence Kinshasa").ville("Kinshasa").build();
        agenceKinshasa.setId(1L);

        agenceGoma = Agence.builder().nom("Agence Goma").ville("Goma").build();
        agenceGoma.setId(2L);

        agenceLubumbashi = Agence.builder().nom("Agence Lubumbashi").ville("Lubumbashi").build();
        agenceLubumbashi.setId(3L);

        agent = Utilisateur.builder()
                .nom("Lumbu")
                .prenom("Louange")
                .email("louange@transitea.cd")
                .role(Role.AGENT)
                .agence(agenceKinshasa)
                .build();
        agent.setId(1L);

        autreAgent = Utilisateur.builder()
                .nom("Autre")
                .prenom("Agent")
                .email("autre@transitea.cd")
                .role(Role.AGENT)
                .agence(agenceLubumbashi)
                .build();
        autreAgent.setId(2L);

        agentAgenceRetrait = Utilisateur.builder()
                .nom("Kalonji")
                .prenom("Eric")
                .email("eric@transitea.cd")
                .role(Role.AGENT)
                .agence(agenceGoma)
                .build();
        agentAgenceRetrait.setId(5L);

        admin = Utilisateur.builder()
                .nom("Admin")
                .prenom("System")
                .email("admin@transitea.cd")
                .role(Role.ADMIN)
                .build();
        admin.setId(3L);

        operateur = Utilisateur.builder()
                .nom("Mutombo")
                .prenom("Pierre")
                .email("operateur@transitea.cd")
                .role(Role.OPERATEUR)
                .agence(agenceKinshasa)
                .build();
        operateur.setId(4L);

        colis = Colis.builder()
                .codeTracking("TRA-2026-ABC123")
                .agenceOrigine(agenceKinshasa)
                .agenceRetrait(agenceGoma)
                .creePar(agent)
                .expediteurNom("Jean Dupont")
                .destinataireNom("Marie Martin")
                .poids(new BigDecimal("2.500"))
                .build();
        colis.setId(10L);

        colisReponse = new ColisReponse(
                10L, "uuid-test", "TRA-2026-ABC123",
                1L, "Agence Kinshasa", 2L, "Agence Goma",
                "Jean Dupont", null, null,
                "Marie Martin", null, null, null, null,
                null, new BigDecimal("2.500"),
                StatutColis.ENREGISTRE, null, 0, null, List.of()
        );
    }

    // --- creer ---

    @Test
    void doit_sauvegarder_colis_et_enregistrer_historique_quand_requete_valide() {
        CreationColisRequete requete = new CreationColisRequete(
                1L, 2L,
                "Jean Dupont", "0812345678", null,
                "Marie Martin", "0812345679", null,
                "123 Rue Principale", "Kinshasa",
                "Documents", new BigDecimal("2.500"), null
        );
        when(agenceRepository.findByIdAndSupprimeFalse(1L)).thenReturn(Optional.of(agenceKinshasa));
        when(agenceRepository.findByIdAndSupprimeFalse(2L)).thenReturn(Optional.of(agenceGoma));
        when(colisRepository.findByCodeTrackingAndSupprimeFalse(anyString()))
                .thenReturn(Optional.empty());
        when(colisRepository.save(any(Colis.class))).thenReturn(colis);
        when(colisMapper.versReponse(any(Colis.class))).thenReturn(colisReponse);

        ColisReponse resultat = colisService.creer(requete, agent);

        assertThat(resultat).isNotNull();
        assertThat(resultat.codeTracking()).isEqualTo("TRA-2026-ABC123");
        verify(colisRepository).save(any(Colis.class));
        verify(miseAJourStatutRepository).save(any(MiseAJourStatut.class));
    }

    @Test
    void doit_lancer_exception_quand_code_tracking_non_unique_apres_max_tentatives() {
        CreationColisRequete requete = new CreationColisRequete(
                1L, 2L, "Jean", null, null, "Marie", null, null,
                null, null, null, null, null
        );
        when(agenceRepository.findByIdAndSupprimeFalse(1L)).thenReturn(Optional.of(agenceKinshasa));
        when(agenceRepository.findByIdAndSupprimeFalse(2L)).thenReturn(Optional.of(agenceGoma));
        when(colisRepository.findByCodeTrackingAndSupprimeFalse(anyString()))
                .thenReturn(Optional.of(colis));

        assertThatThrownBy(() -> colisService.creer(requete, agent))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Impossible de generer un code tracking unique");

        verify(colisRepository, never()).save(any(Colis.class));
    }

    // --- lister ---

    @Test
    void doit_retourner_les_colis_de_lagence_quand_statut_null_et_agent() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Colis> page = new PageImpl<>(List.of(colis));
        when(colisRepository.findByAgenceAndSupprimeFalse(agenceKinshasa, pageable))
                .thenReturn(page);
        when(colisMapper.versReponse(any(Colis.class))).thenReturn(colisReponse);

        ReponsePagee<ColisReponse> resultat = colisService.lister(agent, null, pageable);

        assertThat(resultat.contenu()).hasSize(1);
        verify(colisRepository).findByAgenceAndSupprimeFalse(agenceKinshasa, pageable);
        verify(colisRepository, never()).findBySupprimeFalse(any());
    }

    @Test
    void doit_filtrer_par_statut_quand_statut_fourni_et_agent() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Colis> page = new PageImpl<>(List.of(colis));
        when(colisRepository.findByAgenceAndStatutActuelAndSupprimeFalse(
                agenceKinshasa, StatutColis.ENREGISTRE, pageable))
                .thenReturn(page);
        when(colisMapper.versReponse(any(Colis.class))).thenReturn(colisReponse);

        ReponsePagee<ColisReponse> resultat =
                colisService.lister(agent, StatutColis.ENREGISTRE, pageable);

        assertThat(resultat.contenu()).hasSize(1);
        verify(colisRepository).findByAgenceAndStatutActuelAndSupprimeFalse(
                agenceKinshasa, StatutColis.ENREGISTRE, pageable);
        verify(colisRepository, never()).findByAgenceAndSupprimeFalse(any(), any());
    }

    @Test
    void doit_retourner_tous_les_colis_quand_admin_liste() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Colis> page = new PageImpl<>(List.of(colis));
        when(colisRepository.findBySupprimeFalse(pageable)).thenReturn(page);
        when(colisMapper.versReponse(any(Colis.class))).thenReturn(colisReponse);

        ReponsePagee<ColisReponse> resultat = colisService.lister(admin, null, pageable);

        assertThat(resultat.contenu()).hasSize(1);
        verify(colisRepository).findBySupprimeFalse(pageable);
        verify(colisRepository, never()).findByAgenceAndSupprimeFalse(any(), any());
    }

    @Test
    void doit_retourner_les_colis_de_lagence_quand_operateur_liste() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Colis> page = new PageImpl<>(List.of(colis));
        when(colisRepository.findByAgenceAndSupprimeFalse(agenceKinshasa, pageable)).thenReturn(page);
        when(colisMapper.versReponse(any(Colis.class))).thenReturn(colisReponse);

        ReponsePagee<ColisReponse> resultat = colisService.lister(operateur, null, pageable);

        assertThat(resultat.contenu()).hasSize(1);
        verify(colisRepository).findByAgenceAndSupprimeFalse(agenceKinshasa, pageable);
    }

    // --- rechercher ---

    @Test
    void doit_rechercher_dans_son_agence_quand_agent() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Colis> page = new PageImpl<>(List.of(colis));
        when(colisRepository.rechercherParAgence(agenceKinshasa, "Marie", pageable))
                .thenReturn(page);
        when(colisMapper.versReponse(any(Colis.class))).thenReturn(colisReponse);

        ReponsePagee<ColisReponse> resultat = colisService.rechercher(agent, "Marie", pageable);

        assertThat(resultat.contenu()).hasSize(1);
        verify(colisRepository).rechercherParAgence(agenceKinshasa, "Marie", pageable);
        verify(colisRepository, never()).rechercherTous(anyString(), any());
    }

    @Test
    void doit_rechercher_dans_tous_les_colis_quand_admin() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Colis> page = new PageImpl<>(List.of(colis));
        when(colisRepository.rechercherTous("Marie", pageable)).thenReturn(page);
        when(colisMapper.versReponse(any(Colis.class))).thenReturn(colisReponse);

        ReponsePagee<ColisReponse> resultat = colisService.rechercher(admin, "Marie", pageable);

        assertThat(resultat.contenu()).hasSize(1);
        verify(colisRepository).rechercherTous("Marie", pageable);
        verify(colisRepository, never()).rechercherParAgence(any(), anyString(), any());
    }

    @Test
    void doit_rechercher_dans_son_agence_quand_operateur() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Colis> page = new PageImpl<>(List.of(colis));
        when(colisRepository.rechercherParAgence(agenceKinshasa, "TRA-2026", pageable)).thenReturn(page);
        when(colisMapper.versReponse(any(Colis.class))).thenReturn(colisReponse);

        ReponsePagee<ColisReponse> resultat = colisService.rechercher(operateur, "TRA-2026", pageable);

        assertThat(resultat.contenu()).hasSize(1);
        verify(colisRepository).rechercherParAgence(agenceKinshasa, "TRA-2026", pageable);
    }

    // --- trouverParId ---

    @Test
    void doit_retourner_colis_quand_il_concerne_lagence_de_lagent() {
        when(colisRepository.findById(10L)).thenReturn(Optional.of(colis));
        when(miseAJourStatutRepository.findByColisOrderByDateCreationAsc(colis))
                .thenReturn(List.of());
        when(colisMapper.versReponse(colis)).thenReturn(colisReponse);
        when(colisMapper.versMiseAJourReponses(any())).thenReturn(List.of());

        ColisReponse resultat = colisService.trouverParId(10L, agent);

        assertThat(resultat).isNotNull();
        assertThat(resultat.id()).isEqualTo(10L);
    }

    @Test
    void doit_autoriser_admin_pour_nimporte_quel_colis() {
        when(colisRepository.findById(10L)).thenReturn(Optional.of(colis));
        when(miseAJourStatutRepository.findByColisOrderByDateCreationAsc(colis))
                .thenReturn(List.of());
        when(colisMapper.versReponse(colis)).thenReturn(colisReponse);
        when(colisMapper.versMiseAJourReponses(any())).thenReturn(List.of());

        ColisReponse resultat = colisService.trouverParId(10L, admin);

        assertThat(resultat).isNotNull();
    }

    @Test
    void doit_autoriser_operateur_de_lagence_concernee() {
        when(colisRepository.findById(10L)).thenReturn(Optional.of(colis));
        when(miseAJourStatutRepository.findByColisOrderByDateCreationAsc(colis))
                .thenReturn(List.of());
        when(colisMapper.versReponse(colis)).thenReturn(colisReponse);
        when(colisMapper.versMiseAJourReponses(any())).thenReturn(List.of());

        ColisReponse resultat = colisService.trouverParId(10L, operateur);

        assertThat(resultat).isNotNull();
    }

    @Test
    void doit_lancer_entite_non_trouvee_exception_quand_colis_inexistant() {
        when(colisRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> colisService.trouverParId(99L, agent))
                .isInstanceOf(EntiteNonTrouveeException.class);
    }

    @Test
    void doit_lancer_acces_non_autorise_exception_quand_agent_dune_autre_agence() {
        when(colisRepository.findById(10L)).thenReturn(Optional.of(colis));

        assertThatThrownBy(() -> colisService.trouverParId(10L, autreAgent))
                .isInstanceOf(AccesNonAutoriseException.class);
    }

    // --- mettreAJourStatut ---

    @Test
    void doit_mettre_a_jour_statut_quand_transition_valide() {
        MiseAJourStatutRequete requete = new MiseAJourStatutRequete(
                StatutColis.EN_TRANSIT, "Kinshasa", "Prise en charge");
        when(colisRepository.findById(10L)).thenReturn(Optional.of(colis));
        when(colisRepository.save(any(Colis.class))).thenReturn(colis);
        when(colisMapper.versReponse(any(Colis.class))).thenReturn(colisReponse);

        colisService.mettreAJourStatut(10L, requete, agent);

        ArgumentCaptor<MiseAJourStatut> captor = ArgumentCaptor.forClass(MiseAJourStatut.class);
        verify(miseAJourStatutRepository).save(captor.capture());
        assertThat(captor.getValue().getStatut()).isEqualTo(StatutColis.EN_TRANSIT);
        assertThat(captor.getValue().getAncienStatut()).isEqualTo(StatutColis.ENREGISTRE);
    }

    @Test
    void doit_autoriser_operateur_a_mettre_a_jour_statut() {
        MiseAJourStatutRequete requete = new MiseAJourStatutRequete(
                StatutColis.EN_TRANSIT, "Kinshasa", null);
        when(colisRepository.findById(10L)).thenReturn(Optional.of(colis));
        when(colisRepository.save(any(Colis.class))).thenReturn(colis);
        when(colisMapper.versReponse(any(Colis.class))).thenReturn(colisReponse);

        colisService.mettreAJourStatut(10L, requete, operateur);

        verify(miseAJourStatutRepository).save(any(MiseAJourStatut.class));
    }

    @Test
    void doit_lancer_exception_quand_transition_statut_interdite() {
        MiseAJourStatutRequete requete = new MiseAJourStatutRequete(
                StatutColis.RETIRE, null, null);
        when(colisRepository.findById(10L)).thenReturn(Optional.of(colis));

        assertThatThrownBy(() -> colisService.mettreAJourStatut(10L, requete, agent))
                .isInstanceOf(TransitionStatutInvalideException.class);

        verify(colisRepository, never()).save(any());
    }

    @Test
    void doit_lancer_acces_non_autorise_exception_quand_mise_a_jour_par_agent_dune_autre_agence() {
        MiseAJourStatutRequete requete = new MiseAJourStatutRequete(
                StatutColis.EN_TRANSIT, null, null);
        when(colisRepository.findById(10L)).thenReturn(Optional.of(colis));

        assertThatThrownBy(() -> colisService.mettreAJourStatut(10L, requete, autreAgent))
                .isInstanceOf(AccesNonAutoriseException.class);
    }

    // --- retirer ---

    @Test
    void doit_retirer_colis_arrive_a_lagence() {
        colis.setStatutActuel(StatutColis.ARRIVE_AGENCE);
        when(colisRepository.findByCodeTrackingAndSupprimeFalse("TRA-2026-ABC123"))
                .thenReturn(Optional.of(colis));
        when(colisRepository.save(any(Colis.class))).thenReturn(colis);
        when(colisMapper.versReponse(any(Colis.class))).thenReturn(colisReponse);

        colisService.retirer("TRA-2026-ABC123", agentAgenceRetrait);

        ArgumentCaptor<Colis> captor = ArgumentCaptor.forClass(Colis.class);
        verify(colisRepository).save(captor.capture());
        assertThat(captor.getValue().getStatutActuel()).isEqualTo(StatutColis.RETIRE);
        verify(notificationService).notifierChangementStatut(any(Colis.class), any());
    }

    @Test
    void doit_autoriser_admin_a_retirer_nimporte_quel_colis() {
        colis.setStatutActuel(StatutColis.ARRIVE_AGENCE);
        when(colisRepository.findByCodeTrackingAndSupprimeFalse("TRA-2026-ABC123"))
                .thenReturn(Optional.of(colis));
        when(colisRepository.save(any(Colis.class))).thenReturn(colis);
        when(colisMapper.versReponse(any(Colis.class))).thenReturn(colisReponse);

        colisService.retirer("TRA-2026-ABC123", admin);

        verify(colisRepository).save(any(Colis.class));
    }

    @Test
    void doit_refuser_retrait_par_agent_de_lagence_origine() {
        colis.setStatutActuel(StatutColis.ARRIVE_AGENCE);
        when(colisRepository.findByCodeTrackingAndSupprimeFalse("TRA-2026-ABC123"))
                .thenReturn(Optional.of(colis));

        assertThatThrownBy(() -> colisService.retirer("TRA-2026-ABC123", agent))
                .isInstanceOf(AccesNonAutoriseException.class);

        verify(colisRepository, never()).save(any());
    }

    @Test
    void doit_lancer_exception_quand_retrait_sur_statut_non_arrive() {
        colis.setStatutActuel(StatutColis.ENREGISTRE);
        when(colisRepository.findByCodeTrackingAndSupprimeFalse("TRA-2026-ABC123"))
                .thenReturn(Optional.of(colis));

        assertThatThrownBy(() -> colisService.retirer("TRA-2026-ABC123", agentAgenceRetrait))
                .isInstanceOf(TransitionStatutInvalideException.class);
    }

    // --- supprimer ---

    @Test
    void doit_appliquer_soft_delete_quand_agent_de_lagence_concernee() {
        when(colisRepository.findById(10L)).thenReturn(Optional.of(colis));
        when(colisRepository.save(any(Colis.class))).thenReturn(colis);

        colisService.supprimer(10L, agent);

        ArgumentCaptor<Colis> captor = ArgumentCaptor.forClass(Colis.class);
        verify(colisRepository).save(captor.capture());
        assertThat(captor.getValue().getSupprime()).isTrue();
    }

    @Test
    void doit_lancer_acces_non_autorise_exception_quand_suppression_par_agent_dune_autre_agence() {
        when(colisRepository.findById(10L)).thenReturn(Optional.of(colis));

        assertThatThrownBy(() -> colisService.supprimer(10L, autreAgent))
                .isInstanceOf(AccesNonAutoriseException.class);

        verify(colisRepository, never()).save(any());
    }

    @Test
    void doit_lancer_entite_non_trouvee_exception_quand_colis_deja_supprime() {
        colis.setSupprime(true);
        when(colisRepository.findById(10L)).thenReturn(Optional.of(colis));

        assertThatThrownBy(() -> colisService.supprimer(10L, agent))
                .isInstanceOf(EntiteNonTrouveeException.class);
    }

    @Test
    void doit_autoriser_admin_a_supprimer_nimporte_quel_colis() {
        when(colisRepository.findById(10L)).thenReturn(Optional.of(colis));
        when(colisRepository.save(any(Colis.class))).thenReturn(colis);

        colisService.supprimer(10L, admin);

        verify(colisRepository, times(1)).save(any(Colis.class));
    }

    @Test
    void doit_autoriser_operateur_a_supprimer_colis_de_son_agence() {
        when(colisRepository.findById(10L)).thenReturn(Optional.of(colis));
        when(colisRepository.save(any(Colis.class))).thenReturn(colis);

        colisService.supprimer(10L, operateur);

        verify(colisRepository, times(1)).save(any(Colis.class));
    }

    // --- genererQrCode ---

    @Test
    void doit_generer_qrcode_quand_agent_de_lagence_concernee() {
        byte[] qrCodeBytes = new byte[]{1, 2, 3};
        String urlTracking = "http://localhost:8080/v1/tracking/TRA-2026-ABC123";
        when(colisRepository.findById(10L)).thenReturn(Optional.of(colis));
        when(qrCodeService.generer(eq(urlTracking), anyString())).thenReturn(qrCodeBytes);

        byte[] resultat = colisService.genererQrCode(10L, agent);

        assertThat(resultat).isEqualTo(qrCodeBytes);
        verify(qrCodeService).generer(eq(urlTracking), anyString());
    }

    @Test
    void doit_generer_qrcode_quand_admin() {
        byte[] qrCodeBytes = new byte[]{1, 2, 3};
        String urlTracking = "http://localhost:8080/v1/tracking/TRA-2026-ABC123";
        when(colisRepository.findById(10L)).thenReturn(Optional.of(colis));
        when(qrCodeService.generer(eq(urlTracking), anyString())).thenReturn(qrCodeBytes);

        byte[] resultat = colisService.genererQrCode(10L, admin);

        assertThat(resultat).isEqualTo(qrCodeBytes);
    }

    @Test
    void doit_generer_qrcode_quand_operateur_de_lagence_concernee() {
        byte[] qrCodeBytes = new byte[]{1, 2, 3};
        String urlTracking = "http://localhost:8080/v1/tracking/TRA-2026-ABC123";
        when(colisRepository.findById(10L)).thenReturn(Optional.of(colis));
        when(qrCodeService.generer(eq(urlTracking), anyString())).thenReturn(qrCodeBytes);

        byte[] resultat = colisService.genererQrCode(10L, operateur);

        assertThat(resultat).isEqualTo(qrCodeBytes);
    }

    @Test
    void doit_lancer_acces_non_autorise_exception_quand_qrcode_demande_par_agent_dune_autre_agence() {
        when(colisRepository.findById(10L)).thenReturn(Optional.of(colis));

        assertThatThrownBy(() -> colisService.genererQrCode(10L, autreAgent))
                .isInstanceOf(AccesNonAutoriseException.class);

        verify(qrCodeService, never()).generer(anyString(), anyString());
    }

    @Test
    void doit_lancer_entite_non_trouvee_exception_quand_qrcode_pour_colis_inexistant() {
        when(colisRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> colisService.genererQrCode(99L, agent))
                .isInstanceOf(EntiteNonTrouveeException.class);
    }

    // --- obtenirStatistiques ---

    @Test
    void doit_compter_par_statut_pour_lagence_de_lagent() {
        when(colisRepository.countByAgenceAndStatutActuelAndSupprimeFalse(
                any(), any())).thenReturn(2L);

        StatistiquesReponse resultat = colisService.obtenirStatistiques(agent);

        assertThat(resultat).isNotNull();
        assertThat(resultat.parStatut()).containsKey(StatutColis.ENREGISTRE);
        assertThat(resultat.total()).isGreaterThanOrEqualTo(0);
        verify(colisRepository, never()).countByStatutActuelAndSupprimeFalse(any());
    }

    @Test
    void doit_compter_tous_les_colis_par_statut_pour_admin() {
        when(colisRepository.countByStatutActuelAndSupprimeFalse(any())).thenReturn(3L);

        StatistiquesReponse resultat = colisService.obtenirStatistiques(admin);

        assertThat(resultat).isNotNull();
        assertThat(resultat.parStatut()).containsKey(StatutColis.EN_TRANSIT);
        verify(colisRepository, never())
                .countByAgenceAndStatutActuelAndSupprimeFalse(any(), any());
    }

    @Test
    void doit_compter_par_statut_pour_lagence_de_loperateur() {
        when(colisRepository.countByAgenceAndStatutActuelAndSupprimeFalse(any(), any())).thenReturn(1L);

        StatistiquesReponse resultat = colisService.obtenirStatistiques(operateur);

        assertThat(resultat.parStatut()).hasSize(StatutColis.values().length);
        verify(colisRepository, never())
                .countByStatutActuelAndSupprimeFalse(any());
    }
}
