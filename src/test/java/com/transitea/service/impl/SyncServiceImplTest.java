package com.transitea.service.impl;

import com.transitea.dto.request.CreationColisRequete;
import com.transitea.dto.request.SyncRequete;
import com.transitea.dto.response.SyncReponse;
import com.transitea.entity.Colis;
import com.transitea.entity.MiseAJourStatut;
import com.transitea.entity.SyncLog;
import com.transitea.entity.Utilisateur;
import com.transitea.entity.enums.Role;
import com.transitea.entity.enums.StatutColis;
import com.transitea.repository.ColisRepository;
import com.transitea.repository.MiseAJourStatutRepository;
import com.transitea.repository.SyncLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SyncServiceImplTest {

    @Mock
    private ColisRepository colisRepository;

    @Mock
    private MiseAJourStatutRepository miseAJourStatutRepository;

    @Mock
    private SyncLogRepository syncLogRepository;

    @InjectMocks
    private SyncServiceImpl syncService;

    private Utilisateur transporteur;
    private CreationColisRequete colisRequete1;
    private CreationColisRequete colisRequete2;
    private Colis colisSauvegarde;

    @BeforeEach
    void initialiser() {
        transporteur = Utilisateur.builder()
                .nom("Lumbu")
                .prenom("Louange")
                .email("transport@transitea.cd")
                .role(Role.TRANSPORTEUR)
                .build();
        transporteur.setId(1L);

        colisRequete1 = new CreationColisRequete(
                "Jean Dupont", "0812345678", "jean@exemple.cd",
                "Marie Martin", "0812345679", "marie@exemple.cd",
                "Avenue Kasa-Vubu 12", "Kinshasa",
                "Documents", new BigDecimal("2.500"), 101L
        );

        colisRequete2 = new CreationColisRequete(
                "Paul Kasongo", "0812345680", null,
                "Fatou Mbeki", "0812345681", null,
                "Quartier Makutano", "Goma",
                "Vetements", new BigDecimal("5.000"), 102L
        );

        colisSauvegarde = Colis.builder()
                .codeTracking("TRA-2026-ABC123")
                .transporteur(transporteur)
                .expediteurNom("Jean Dupont")
                .destinataireNom("Marie Martin")
                .poids(new BigDecimal("2.500"))
                .localId(101L)
                .build();
        colisSauvegarde.setId(10L);
    }

    // --- cas nominal ---

    @Test
    void doit_creer_colis_et_enregistrer_log_quand_batch_valide() {
        when(colisRepository.findByCodeTrackingAndSupprimeFalse(anyString()))
                .thenReturn(Optional.empty());
        when(colisRepository.findByTransporteurAndLocalIdAndSupprimeFalse(any(), any()))
                .thenReturn(Optional.empty());
        when(colisRepository.save(any(Colis.class))).thenReturn(colisSauvegarde);

        SyncReponse reponse = syncService.synchroniser(
                new SyncRequete(List.of(colisRequete1)), transporteur);

        assertThat(reponse.nbEnvoyes()).isEqualTo(1);
        assertThat(reponse.nbReussis()).isEqualTo(1);
        assertThat(reponse.nbDoublons()).isEqualTo(0);
        assertThat(reponse.nbEchecs()).isEqualTo(0);
        assertThat(reponse.resultats()).hasSize(1);
        assertThat(reponse.resultats().get(0).succes()).isTrue();
        assertThat(reponse.resultats().get(0).codeTracking()).isEqualTo("TRA-2026-ABC123");

        verify(colisRepository).save(any(Colis.class));
        verify(miseAJourStatutRepository).save(any(MiseAJourStatut.class));
        verify(syncLogRepository).save(any(SyncLog.class));
    }

    @Test
    void doit_creer_plusieurs_colis_en_un_seul_appel() {
        Colis colisSauvegarde2 = Colis.builder()
                .codeTracking("TRA-2026-XYZ999")
                .transporteur(transporteur)
                .expediteurNom("Paul Kasongo")
                .destinataireNom("Fatou Mbeki")
                .poids(new BigDecimal("5.000"))
                .localId(102L)
                .build();
        colisSauvegarde2.setId(11L);

        when(colisRepository.findByCodeTrackingAndSupprimeFalse(anyString()))
                .thenReturn(Optional.empty());
        when(colisRepository.findByTransporteurAndLocalIdAndSupprimeFalse(any(), any()))
                .thenReturn(Optional.empty());
        when(colisRepository.save(any(Colis.class)))
                .thenReturn(colisSauvegarde)
                .thenReturn(colisSauvegarde2);

        SyncReponse reponse = syncService.synchroniser(
                new SyncRequete(List.of(colisRequete1, colisRequete2)), transporteur);

        assertThat(reponse.nbEnvoyes()).isEqualTo(2);
        assertThat(reponse.nbReussis()).isEqualTo(2);
        assertThat(reponse.resultats()).hasSize(2);
        verify(colisRepository, times(2)).save(any(Colis.class));
        verify(miseAJourStatutRepository, times(2)).save(any(MiseAJourStatut.class));
    }

    // --- doublons ---

    @Test
    void doit_detecter_doublon_quand_local_id_deja_synchronise() {
        when(colisRepository.findByTransporteurAndLocalIdAndSupprimeFalse(
                transporteur, 101L))
                .thenReturn(Optional.of(colisSauvegarde));

        SyncReponse reponse = syncService.synchroniser(
                new SyncRequete(List.of(colisRequete1)), transporteur);

        assertThat(reponse.nbDoublons()).isEqualTo(1);
        assertThat(reponse.nbReussis()).isEqualTo(0);
        assertThat(reponse.resultats().get(0).doublon()).isTrue();
        assertThat(reponse.resultats().get(0).codeTracking()).isEqualTo("TRA-2026-ABC123");
        assertThat(reponse.resultats().get(0).succes()).isTrue();

        verify(colisRepository, never()).save(any(Colis.class));
    }

    @Test
    void doit_traiter_mix_nouveaux_et_doublons() {
        when(colisRepository.findByTransporteurAndLocalIdAndSupprimeFalse(
                transporteur, 101L))
                .thenReturn(Optional.of(colisSauvegarde));
        when(colisRepository.findByTransporteurAndLocalIdAndSupprimeFalse(
                transporteur, 102L))
                .thenReturn(Optional.empty());
        when(colisRepository.findByCodeTrackingAndSupprimeFalse(anyString()))
                .thenReturn(Optional.empty());

        Colis colisSauvegarde2 = Colis.builder()
                .codeTracking("TRA-2026-XYZ999")
                .transporteur(transporteur)
                .poids(new BigDecimal("5.000"))
                .localId(102L)
                .build();
        colisSauvegarde2.setId(11L);
        when(colisRepository.save(any(Colis.class))).thenReturn(colisSauvegarde2);

        SyncReponse reponse = syncService.synchroniser(
                new SyncRequete(List.of(colisRequete1, colisRequete2)), transporteur);

        assertThat(reponse.nbEnvoyes()).isEqualTo(2);
        assertThat(reponse.nbReussis()).isEqualTo(1);
        assertThat(reponse.nbDoublons()).isEqualTo(1);
        assertThat(reponse.nbEchecs()).isEqualTo(0);
        verify(colisRepository, times(1)).save(any(Colis.class));
    }

    @Test
    void doit_creer_colis_sans_verifier_doublon_quand_local_id_null() {
        CreationColisRequete requeteSansLocalId = new CreationColisRequete(
                "Jean", null, null, "Marie", null, null,
                null, null, null, null, null
        );

        when(colisRepository.findByCodeTrackingAndSupprimeFalse(anyString()))
                .thenReturn(Optional.empty());
        when(colisRepository.save(any(Colis.class))).thenReturn(colisSauvegarde);

        SyncReponse reponse = syncService.synchroniser(
                new SyncRequete(List.of(requeteSansLocalId)), transporteur);

        assertThat(reponse.nbReussis()).isEqualTo(1);
        verify(colisRepository, never()).findByTransporteurAndLocalIdAndSupprimeFalse(any(), any());
    }

    // --- enregistrement historique ---

    @Test
    void doit_enregistrer_historique_statut_enregistre_pour_chaque_colis_cree() {
        when(colisRepository.findByCodeTrackingAndSupprimeFalse(anyString()))
                .thenReturn(Optional.empty());
        when(colisRepository.findByTransporteurAndLocalIdAndSupprimeFalse(any(), any()))
                .thenReturn(Optional.empty());
        when(colisRepository.save(any(Colis.class))).thenReturn(colisSauvegarde);

        syncService.synchroniser(new SyncRequete(List.of(colisRequete1)), transporteur);

        ArgumentCaptor<MiseAJourStatut> captor = ArgumentCaptor.forClass(MiseAJourStatut.class);
        verify(miseAJourStatutRepository).save(captor.capture());
        assertThat(captor.getValue().getStatut()).isEqualTo(StatutColis.ENREGISTRE);
        assertThat(captor.getValue().getAncienStatut()).isNull();
    }

    // --- SyncLog ---

    @Test
    void doit_sauvegarder_synclog_avec_compteurs_corrects() {
        when(colisRepository.findByCodeTrackingAndSupprimeFalse(anyString()))
                .thenReturn(Optional.empty());
        when(colisRepository.findByTransporteurAndLocalIdAndSupprimeFalse(any(), any()))
                .thenReturn(Optional.empty());
        when(colisRepository.save(any(Colis.class))).thenReturn(colisSauvegarde);

        syncService.synchroniser(
                new SyncRequete(List.of(colisRequete1)), transporteur);

        ArgumentCaptor<SyncLog> captor = ArgumentCaptor.forClass(SyncLog.class);
        verify(syncLogRepository).save(captor.capture());

        SyncLog log = captor.getValue();
        assertThat(log.getTransporteur()).isEqualTo(transporteur);
        assertThat(log.getNbColisEnvoyes()).isEqualTo(1);
        assertThat(log.getNbColisReussis()).isEqualTo(1);
        assertThat(log.getNbColisEchecs()).isEqualTo(0);
        assertThat(log.getDateDebut()).isNotNull();
        assertThat(log.getDateFin()).isNotNull();
    }

    @Test
    void doit_toujours_sauvegarder_synclog_meme_si_tous_doublons() {
        when(colisRepository.findByTransporteurAndLocalIdAndSupprimeFalse(any(), any()))
                .thenReturn(Optional.of(colisSauvegarde));

        syncService.synchroniser(
                new SyncRequete(List.of(colisRequete1)), transporteur);

        verify(syncLogRepository).save(any(SyncLog.class));
    }

    // --- gestion erreurs ---

    @Test
    void doit_marquer_echec_et_continuer_quand_creation_echoue() {
        when(colisRepository.findByTransporteurAndLocalIdAndSupprimeFalse(any(), any()))
                .thenReturn(Optional.empty());
        when(colisRepository.findByCodeTrackingAndSupprimeFalse(anyString()))
                .thenReturn(Optional.empty());
        when(colisRepository.save(any(Colis.class)))
                .thenThrow(new RuntimeException("Erreur BDD"))
                .thenReturn(colisSauvegarde);

        Colis colisSauvegarde2 = Colis.builder()
                .codeTracking("TRA-2026-XYZ999")
                .transporteur(transporteur)
                .poids(new BigDecimal("5.000"))
                .localId(102L)
                .build();
        colisSauvegarde2.setId(11L);

        when(colisRepository.findByTransporteurAndLocalIdAndSupprimeFalse(
                eq(transporteur), eq(102L)))
                .thenReturn(Optional.empty());

        SyncReponse reponse = syncService.synchroniser(
                new SyncRequete(List.of(colisRequete1, colisRequete2)), transporteur);

        assertThat(reponse.nbEchecs()).isEqualTo(1);
        assertThat(reponse.resultats().get(0).succes()).isFalse();
        assertThat(reponse.resultats().get(0).erreur()).isNotBlank();
        verify(syncLogRepository).save(any(SyncLog.class));
    }
}
