package com.transitea.service.impl;

import com.transitea.dto.request.CreationColisRequete;
import com.transitea.dto.request.MiseAJourStatutRequete;
import com.transitea.dto.response.ColisReponse;
import com.transitea.dto.response.ReponsePagee;
import com.transitea.entity.Colis;
import com.transitea.entity.MiseAJourStatut;
import com.transitea.entity.Utilisateur;
import com.transitea.entity.enums.Role;
import com.transitea.entity.enums.StatutColis;
import com.transitea.exception.AccesNonAutoriseException;
import com.transitea.exception.EntiteNonTrouveeException;
import com.transitea.exception.TransitionStatutInvalideException;
import com.transitea.mapper.ColisMapper;
import com.transitea.repository.ColisRepository;
import com.transitea.repository.MiseAJourStatutRepository;
import com.transitea.service.NotificationService;
import com.transitea.service.QrCodeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ColisServiceImplTest {

    @Mock
    private ColisRepository colisRepository;

    @Mock
    private MiseAJourStatutRepository miseAJourStatutRepository;

    @Mock
    private ColisMapper colisMapper;

    @Mock
    private QrCodeService qrCodeService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ColisServiceImpl colisService;

    private Utilisateur transporteur;
    private Utilisateur autreTransporteur;
    private Utilisateur admin;
    private Colis colis;
    private ColisReponse colisReponse;

    @BeforeEach
    void initialiser() {
        ReflectionTestUtils.setField(colisService, "baseUrl", "http://localhost:8080");

        transporteur = Utilisateur.builder()
                .nom("Lumbu")
                .prenom("Louange")
                .email("louange@transitea.cd")
                .role(Role.TRANSPORTEUR)
                .build();
        transporteur.setId(1L);

        autreTransporteur = Utilisateur.builder()
                .nom("Autre")
                .prenom("Transporteur")
                .email("autre@transitea.cd")
                .role(Role.TRANSPORTEUR)
                .build();
        autreTransporteur.setId(2L);

        admin = Utilisateur.builder()
                .nom("Admin")
                .prenom("System")
                .email("admin@transitea.cd")
                .role(Role.ADMIN)
                .build();
        admin.setId(3L);

        colis = Colis.builder()
                .codeTracking("TRA-2026-ABC123")
                .transporteur(transporteur)
                .expediteurNom("Jean Dupont")
                .destinataireNom("Marie Martin")
                .poids(new BigDecimal("2.500"))
                .build();
        colis.setId(10L);

        colisReponse = new ColisReponse(
                10L, "uuid-test", "TRA-2026-ABC123",
                1L, "Louange Lumbu",
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
                "Jean Dupont", "0812345678", null,
                "Marie Martin", "0812345679", null,
                "123 Rue Principale", "Kinshasa",
                "Documents", new BigDecimal("2.500"), null
        );
        when(colisRepository.findByCodeTrackingAndSupprimeFalse(anyString()))
                .thenReturn(Optional.empty());
        when(colisRepository.save(any(Colis.class))).thenReturn(colis);
        when(colisMapper.versReponse(any(Colis.class))).thenReturn(colisReponse);

        ColisReponse resultat = colisService.creer(requete, transporteur);

        assertThat(resultat).isNotNull();
        assertThat(resultat.codeTracking()).isEqualTo("TRA-2026-ABC123");
        verify(colisRepository).save(any(Colis.class));
        verify(miseAJourStatutRepository).save(any(MiseAJourStatut.class));
    }

    @Test
    void doit_lancer_exception_quand_code_tracking_non_unique_apres_max_tentatives() {
        CreationColisRequete requete = new CreationColisRequete(
                "Jean", null, null, "Marie", null, null,
                null, null, null, null, null
        );
        when(colisRepository.findByCodeTrackingAndSupprimeFalse(anyString()))
                .thenReturn(Optional.of(colis));

        assertThatThrownBy(() -> colisService.creer(requete, transporteur))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Impossible de generer un code tracking unique");

        verify(colisRepository, never()).save(any(Colis.class));
    }

    // --- lister ---

    @Test
    void doit_retourner_tous_les_colis_quand_statut_null() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Colis> page = new PageImpl<>(List.of(colis));
        when(colisRepository.findByTransporteurAndSupprimeFalse(transporteur, pageable))
                .thenReturn(page);
        when(colisMapper.versReponse(any(Colis.class))).thenReturn(colisReponse);

        ReponsePagee<ColisReponse> resultat = colisService.lister(transporteur, null, pageable);

        assertThat(resultat.contenu()).hasSize(1);
        verify(colisRepository).findByTransporteurAndSupprimeFalse(transporteur, pageable);
        verify(colisRepository, never())
                .findByTransporteurAndStatutActuelAndSupprimeFalse(any(), any(), any());
    }

    @Test
    void doit_filtrer_par_statut_quand_statut_fourni() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Colis> page = new PageImpl<>(List.of(colis));
        when(colisRepository.findByTransporteurAndStatutActuelAndSupprimeFalse(
                transporteur, StatutColis.ENREGISTRE, pageable))
                .thenReturn(page);
        when(colisMapper.versReponse(any(Colis.class))).thenReturn(colisReponse);

        ReponsePagee<ColisReponse> resultat =
                colisService.lister(transporteur, StatutColis.ENREGISTRE, pageable);

        assertThat(resultat.contenu()).hasSize(1);
        verify(colisRepository).findByTransporteurAndStatutActuelAndSupprimeFalse(
                transporteur, StatutColis.ENREGISTRE, pageable);
        verify(colisRepository, never()).findByTransporteurAndSupprimeFalse(any(), any());
    }

    // --- trouverParId ---

    @Test
    void doit_retourner_colis_quand_il_appartient_au_transporteur() {
        when(colisRepository.findById(10L)).thenReturn(Optional.of(colis));
        when(miseAJourStatutRepository.findByColisOrderByDateCreationAsc(colis))
                .thenReturn(List.of());
        when(colisMapper.versReponse(colis)).thenReturn(colisReponse);
        when(colisMapper.versMiseAJourReponses(any())).thenReturn(List.of());

        ColisReponse resultat = colisService.trouverParId(10L, transporteur);

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
    void doit_lancer_entite_non_trouvee_exception_quand_colis_inexistant() {
        when(colisRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> colisService.trouverParId(99L, transporteur))
                .isInstanceOf(EntiteNonTrouveeException.class);
    }

    @Test
    void doit_lancer_acces_non_autorise_exception_quand_transporteur_non_proprietaire() {
        when(colisRepository.findById(10L)).thenReturn(Optional.of(colis));

        assertThatThrownBy(() -> colisService.trouverParId(10L, autreTransporteur))
                .isInstanceOf(AccesNonAutoriseException.class);
    }

    // --- mettreAJourStatut ---

    @Test
    void doit_mettre_a_jour_statut_quand_transition_valide() {
        MiseAJourStatutRequete requete = new MiseAJourStatutRequete(
                StatutColis.PRIS_EN_CHARGE, "Kinshasa", "Prise en charge");
        when(colisRepository.findById(10L)).thenReturn(Optional.of(colis));
        when(colisRepository.save(any(Colis.class))).thenReturn(colis);
        when(colisMapper.versReponse(any(Colis.class))).thenReturn(colisReponse);

        colisService.mettreAJourStatut(10L, requete, transporteur);

        ArgumentCaptor<MiseAJourStatut> captor = ArgumentCaptor.forClass(MiseAJourStatut.class);
        verify(miseAJourStatutRepository).save(captor.capture());
        assertThat(captor.getValue().getStatut()).isEqualTo(StatutColis.PRIS_EN_CHARGE);
        assertThat(captor.getValue().getAncienStatut()).isEqualTo(StatutColis.ENREGISTRE);
    }

    @Test
    void doit_lancer_exception_quand_transition_statut_interdite() {
        MiseAJourStatutRequete requete = new MiseAJourStatutRequete(
                StatutColis.LIVRE, null, null);
        when(colisRepository.findById(10L)).thenReturn(Optional.of(colis));

        assertThatThrownBy(() -> colisService.mettreAJourStatut(10L, requete, transporteur))
                .isInstanceOf(TransitionStatutInvalideException.class);

        verify(colisRepository, never()).save(any());
    }

    @Test
    void doit_lancer_acces_non_autorise_exception_quand_mise_a_jour_par_non_proprietaire() {
        MiseAJourStatutRequete requete = new MiseAJourStatutRequete(
                StatutColis.PRIS_EN_CHARGE, null, null);
        when(colisRepository.findById(10L)).thenReturn(Optional.of(colis));

        assertThatThrownBy(() -> colisService.mettreAJourStatut(10L, requete, autreTransporteur))
                .isInstanceOf(AccesNonAutoriseException.class);
    }

    // --- supprimer ---

    @Test
    void doit_appliquer_soft_delete_quand_proprietaire() {
        when(colisRepository.findById(10L)).thenReturn(Optional.of(colis));
        when(colisRepository.save(any(Colis.class))).thenReturn(colis);

        colisService.supprimer(10L, transporteur);

        ArgumentCaptor<Colis> captor = ArgumentCaptor.forClass(Colis.class);
        verify(colisRepository).save(captor.capture());
        assertThat(captor.getValue().getSupprime()).isTrue();
    }

    @Test
    void doit_lancer_acces_non_autorise_exception_quand_suppression_par_non_proprietaire() {
        when(colisRepository.findById(10L)).thenReturn(Optional.of(colis));

        assertThatThrownBy(() -> colisService.supprimer(10L, autreTransporteur))
                .isInstanceOf(AccesNonAutoriseException.class);

        verify(colisRepository, never()).save(any());
    }

    @Test
    void doit_lancer_entite_non_trouvee_exception_quand_colis_deja_supprime() {
        colis.setSupprime(true);
        when(colisRepository.findById(10L)).thenReturn(Optional.of(colis));

        assertThatThrownBy(() -> colisService.supprimer(10L, transporteur))
                .isInstanceOf(EntiteNonTrouveeException.class);
    }

    @Test
    void doit_autoriser_admin_a_supprimer_nimporte_quel_colis() {
        when(colisRepository.findById(10L)).thenReturn(Optional.of(colis));
        when(colisRepository.save(any(Colis.class))).thenReturn(colis);

        colisService.supprimer(10L, admin);

        verify(colisRepository, times(1)).save(any(Colis.class));
    }

    // --- genererQrCode ---

    @Test
    void doit_generer_qrcode_quand_proprietaire() {
        byte[] qrCodeBytes = new byte[]{1, 2, 3};
        String urlTracking = "http://localhost:8080/v1/tracking/TRA-2026-ABC123";
        when(colisRepository.findById(10L)).thenReturn(Optional.of(colis));
        when(qrCodeService.generer(urlTracking)).thenReturn(qrCodeBytes);

        byte[] resultat = colisService.genererQrCode(10L, transporteur);

        assertThat(resultat).isEqualTo(qrCodeBytes);
        verify(qrCodeService).generer(urlTracking);
    }

    @Test
    void doit_generer_qrcode_quand_admin() {
        byte[] qrCodeBytes = new byte[]{1, 2, 3};
        String urlTracking = "http://localhost:8080/v1/tracking/TRA-2026-ABC123";
        when(colisRepository.findById(10L)).thenReturn(Optional.of(colis));
        when(qrCodeService.generer(urlTracking)).thenReturn(qrCodeBytes);

        byte[] resultat = colisService.genererQrCode(10L, admin);

        assertThat(resultat).isEqualTo(qrCodeBytes);
    }

    @Test
    void doit_lancer_acces_non_autorise_exception_quand_qrcode_demande_par_non_proprietaire() {
        when(colisRepository.findById(10L)).thenReturn(Optional.of(colis));

        assertThatThrownBy(() -> colisService.genererQrCode(10L, autreTransporteur))
                .isInstanceOf(AccesNonAutoriseException.class);

        verify(qrCodeService, never()).generer(anyString());
    }

    @Test
    void doit_lancer_entite_non_trouvee_exception_quand_qrcode_pour_colis_inexistant() {
        when(colisRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> colisService.genererQrCode(99L, transporteur))
                .isInstanceOf(EntiteNonTrouveeException.class);
    }
}
