package com.transitea.service.impl;

import com.transitea.dto.request.CreationColisRequete;
import com.transitea.dto.request.MiseAJourStatutSyncItem;
import com.transitea.dto.request.SyncUploadRequete;
import com.transitea.dto.response.ColisReponse;
import com.transitea.dto.response.SyncDownloadReponse;
import com.transitea.dto.response.SyncUploadReponse;
import com.transitea.entity.Agence;
import com.transitea.entity.Colis;
import com.transitea.entity.MiseAJourStatut;
import com.transitea.entity.SyncLog;
import com.transitea.entity.Utilisateur;
import com.transitea.entity.enums.Role;
import com.transitea.entity.enums.StatutColis;
import com.transitea.mapper.ColisMapper;
import com.transitea.repository.AgenceRepository;
import com.transitea.repository.ColisRepository;
import com.transitea.repository.MiseAJourStatutRepository;
import com.transitea.repository.SyncLogRepository;
import com.transitea.service.NotificationService;
import com.transitea.service.QuotaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SyncServiceImplTest {

    @Mock
    private ColisRepository colisRepository;

    @Mock
    private AgenceRepository agenceRepository;

    @Mock
    private MiseAJourStatutRepository miseAJourStatutRepository;

    @Mock
    private SyncLogRepository syncLogRepository;

    @Mock
    private QuotaService quotaService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private ColisMapper colisMapper;

    @InjectMocks
    private SyncServiceImpl syncService;

    private Agence agenceKinshasa;
    private Agence agenceGoma;
    private Utilisateur agent;
    private Utilisateur admin;
    private CreationColisRequete colisRequete1;
    private CreationColisRequete colisRequete2;
    private Colis colisSauvegarde;

    @BeforeEach
    void initialiser() {
        agenceKinshasa = Agence.builder().nom("Agence Kinshasa").ville("Kinshasa").build();
        agenceKinshasa.setId(1L);

        agenceGoma = Agence.builder().nom("Agence Goma").ville("Goma").build();
        agenceGoma.setId(2L);

        agent = Utilisateur.builder()
                .nom("Lumbu")
                .prenom("Louange")
                .email("agent@transitea.cd")
                .role(Role.AGENT)
                .agence(agenceKinshasa)
                .build();
        agent.setId(1L);

        admin = Utilisateur.builder()
                .nom("Lalande")
                .prenom("Jean-Paul")
                .email("admin@transitea.cd")
                .role(Role.ADMIN)
                .build();
        admin.setId(2L);

        lenient().when(agenceRepository.findByIdAndSupprimeFalse(1L)).thenReturn(Optional.of(agenceKinshasa));
        lenient().when(agenceRepository.findByIdAndSupprimeFalse(2L)).thenReturn(Optional.of(agenceGoma));

        colisRequete1 = new CreationColisRequete(
                1L, 2L,
                "Jean Dupont", "0812345678", "jean@exemple.cd",
                "Marie Martin", "0812345679", "marie@exemple.cd",
                "Avenue Kasa-Vubu 12", "Kinshasa",
                "Documents", new BigDecimal("2.500"), 101L
        );

        colisRequete2 = new CreationColisRequete(
                1L, 2L,
                "Paul Kasongo", "0812345680", null,
                "Fatou Mbeki", "0812345681", null,
                "Quartier Makutano", "Goma",
                "Vetements", new BigDecimal("5.000"), 102L
        );

        colisSauvegarde = Colis.builder()
                .codeTracking("TRA-2026-ABC123")
                .agenceOrigine(agenceKinshasa)
                .agenceRetrait(agenceGoma)
                .creePar(agent)
                .expediteurNom("Jean Dupont")
                .destinataireNom("Marie Martin")
                .poids(new BigDecimal("2.500"))
                .localId(101L)
                .statutActuel(StatutColis.ENREGISTRE)
                .version(0)
                .build();
        colisSauvegarde.setId(10L);
    }

    private SyncUploadRequete requeteColis(List<CreationColisRequete> colis) {
        return new SyncUploadRequete(colis, List.of());
    }

    private SyncUploadRequete requeteStatuts(List<MiseAJourStatutSyncItem> statuts) {
        return new SyncUploadRequete(List.of(), statuts);
    }

    // --- cas nominal : creation de colis ---

    @Test
    void doit_creer_colis_et_enregistrer_log_quand_batch_valide() {
        when(colisRepository.findByCodeTrackingAndSupprimeFalse(anyString()))
                .thenReturn(Optional.empty());
        when(colisRepository.findByCreeParAndLocalIdAndSupprimeFalse(any(), any()))
                .thenReturn(Optional.empty());
        when(colisRepository.save(any(Colis.class))).thenReturn(colisSauvegarde);

        SyncUploadReponse reponse = syncService.synchroniserUpload(requeteColis(List.of(colisRequete1)), agent);

        assertThat(reponse.nbColisEnvoyes()).isEqualTo(1);
        assertThat(reponse.nbColisReussis()).isEqualTo(1);
        assertThat(reponse.nbColisDoublons()).isEqualTo(0);
        assertThat(reponse.nbColisEchecs()).isEqualTo(0);
        assertThat(reponse.colisResultats()).hasSize(1);
        assertThat(reponse.colisResultats().get(0).succes()).isTrue();
        assertThat(reponse.colisResultats().get(0).codeTracking()).isEqualTo("TRA-2026-ABC123");

        verify(colisRepository).save(any(Colis.class));
        verify(miseAJourStatutRepository).save(any(MiseAJourStatut.class));
        verify(syncLogRepository).save(any(SyncLog.class));
    }

    @Test
    void doit_creer_plusieurs_colis_en_un_seul_appel() {
        Colis colisSauvegarde2 = Colis.builder()
                .codeTracking("TRA-2026-XYZ999")
                .agenceOrigine(agenceKinshasa)
                .agenceRetrait(agenceGoma)
                .creePar(agent)
                .expediteurNom("Paul Kasongo")
                .destinataireNom("Fatou Mbeki")
                .poids(new BigDecimal("5.000"))
                .localId(102L)
                .build();
        colisSauvegarde2.setId(11L);

        when(colisRepository.findByCodeTrackingAndSupprimeFalse(anyString()))
                .thenReturn(Optional.empty());
        when(colisRepository.findByCreeParAndLocalIdAndSupprimeFalse(any(), any()))
                .thenReturn(Optional.empty());
        when(colisRepository.save(any(Colis.class)))
                .thenReturn(colisSauvegarde)
                .thenReturn(colisSauvegarde2);

        SyncUploadReponse reponse =
                syncService.synchroniserUpload(requeteColis(List.of(colisRequete1, colisRequete2)), agent);

        assertThat(reponse.nbColisEnvoyes()).isEqualTo(2);
        assertThat(reponse.nbColisReussis()).isEqualTo(2);
        assertThat(reponse.colisResultats()).hasSize(2);
        verify(colisRepository, times(2)).save(any(Colis.class));
        verify(miseAJourStatutRepository, times(2)).save(any(MiseAJourStatut.class));
    }

    // --- doublons ---

    @Test
    void doit_detecter_doublon_quand_local_id_deja_synchronise() {
        when(colisRepository.findByCreeParAndLocalIdAndSupprimeFalse(
                agent, 101L))
                .thenReturn(Optional.of(colisSauvegarde));

        SyncUploadReponse reponse = syncService.synchroniserUpload(requeteColis(List.of(colisRequete1)), agent);

        assertThat(reponse.nbColisDoublons()).isEqualTo(1);
        assertThat(reponse.nbColisReussis()).isEqualTo(0);
        assertThat(reponse.colisResultats().get(0).doublon()).isTrue();
        assertThat(reponse.colisResultats().get(0).codeTracking()).isEqualTo("TRA-2026-ABC123");
        assertThat(reponse.colisResultats().get(0).succes()).isTrue();

        verify(colisRepository, never()).save(any(Colis.class));
    }

    @Test
    void doit_traiter_mix_nouveaux_et_doublons() {
        when(colisRepository.findByCreeParAndLocalIdAndSupprimeFalse(
                agent, 101L))
                .thenReturn(Optional.of(colisSauvegarde));
        when(colisRepository.findByCreeParAndLocalIdAndSupprimeFalse(
                agent, 102L))
                .thenReturn(Optional.empty());
        when(colisRepository.findByCodeTrackingAndSupprimeFalse(anyString()))
                .thenReturn(Optional.empty());

        Colis colisSauvegarde2 = Colis.builder()
                .codeTracking("TRA-2026-XYZ999")
                .agenceOrigine(agenceKinshasa)
                .agenceRetrait(agenceGoma)
                .creePar(agent)
                .poids(new BigDecimal("5.000"))
                .localId(102L)
                .build();
        colisSauvegarde2.setId(11L);
        when(colisRepository.save(any(Colis.class))).thenReturn(colisSauvegarde2);

        SyncUploadReponse reponse =
                syncService.synchroniserUpload(requeteColis(List.of(colisRequete1, colisRequete2)), agent);

        assertThat(reponse.nbColisEnvoyes()).isEqualTo(2);
        assertThat(reponse.nbColisReussis()).isEqualTo(1);
        assertThat(reponse.nbColisDoublons()).isEqualTo(1);
        assertThat(reponse.nbColisEchecs()).isEqualTo(0);
        verify(colisRepository, times(1)).save(any(Colis.class));
    }

    @Test
    void doit_creer_colis_sans_verifier_doublon_quand_local_id_null() {
        CreationColisRequete requeteSansLocalId = new CreationColisRequete(
                1L, 2L, "Jean", null, null, "Marie", null, null,
                null, null, null, null, null
        );

        when(colisRepository.findByCodeTrackingAndSupprimeFalse(anyString()))
                .thenReturn(Optional.empty());
        when(colisRepository.save(any(Colis.class))).thenReturn(colisSauvegarde);

        SyncUploadReponse reponse = syncService.synchroniserUpload(requeteColis(List.of(requeteSansLocalId)), agent);

        assertThat(reponse.nbColisReussis()).isEqualTo(1);
        verify(colisRepository, never()).findByCreeParAndLocalIdAndSupprimeFalse(any(), any());
    }

    // --- enregistrement historique ---

    @Test
    void doit_enregistrer_historique_statut_enregistre_pour_chaque_colis_cree() {
        when(colisRepository.findByCodeTrackingAndSupprimeFalse(anyString()))
                .thenReturn(Optional.empty());
        when(colisRepository.findByCreeParAndLocalIdAndSupprimeFalse(any(), any()))
                .thenReturn(Optional.empty());
        when(colisRepository.save(any(Colis.class))).thenReturn(colisSauvegarde);

        syncService.synchroniserUpload(requeteColis(List.of(colisRequete1)), agent);

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
        when(colisRepository.findByCreeParAndLocalIdAndSupprimeFalse(any(), any()))
                .thenReturn(Optional.empty());
        when(colisRepository.save(any(Colis.class))).thenReturn(colisSauvegarde);

        syncService.synchroniserUpload(requeteColis(List.of(colisRequete1)), agent);

        ArgumentCaptor<SyncLog> captor = ArgumentCaptor.forClass(SyncLog.class);
        verify(syncLogRepository).save(captor.capture());

        SyncLog log = captor.getValue();
        assertThat(log.getUtilisateur()).isEqualTo(agent);
        assertThat(log.getNbColisEnvoyes()).isEqualTo(1);
        assertThat(log.getNbColisReussis()).isEqualTo(1);
        assertThat(log.getNbColisEchecs()).isEqualTo(0);
        assertThat(log.getDateDebut()).isNotNull();
        assertThat(log.getDateFin()).isNotNull();
    }

    @Test
    void doit_toujours_sauvegarder_synclog_meme_si_tous_doublons() {
        when(colisRepository.findByCreeParAndLocalIdAndSupprimeFalse(any(), any()))
                .thenReturn(Optional.of(colisSauvegarde));

        syncService.synchroniserUpload(requeteColis(List.of(colisRequete1)), agent);

        verify(syncLogRepository).save(any(SyncLog.class));
    }

    // --- gestion erreurs ---

    @Test
    void doit_marquer_echec_et_continuer_quand_creation_echoue() {
        when(colisRepository.findByCreeParAndLocalIdAndSupprimeFalse(any(), any()))
                .thenReturn(Optional.empty());
        when(colisRepository.findByCodeTrackingAndSupprimeFalse(anyString()))
                .thenReturn(Optional.empty());
        when(colisRepository.save(any(Colis.class)))
                .thenThrow(new RuntimeException("Erreur BDD"))
                .thenReturn(colisSauvegarde);

        when(colisRepository.findByCreeParAndLocalIdAndSupprimeFalse(
                eq(agent), eq(102L)))
                .thenReturn(Optional.empty());

        SyncUploadReponse reponse =
                syncService.synchroniserUpload(requeteColis(List.of(colisRequete1, colisRequete2)), agent);

        assertThat(reponse.nbColisEchecs()).isEqualTo(1);
        assertThat(reponse.colisResultats().get(0).succes()).isFalse();
        assertThat(reponse.colisResultats().get(0).erreur()).isNotBlank();
        verify(syncLogRepository).save(any(SyncLog.class));
    }

    // --- mise a jour de statut en sync : cas nominal ---

    @Test
    void doit_appliquer_mise_a_jour_statut_valide() {
        when(colisRepository.findById(10L)).thenReturn(Optional.of(colisSauvegarde));
        when(colisRepository.save(any(Colis.class))).thenReturn(colisSauvegarde);

        MiseAJourStatutSyncItem item = new MiseAJourStatutSyncItem(
                10L, null, StatutColis.EN_TRANSIT, "Depart agence", 0, LocalDateTime.now());

        SyncUploadReponse reponse = syncService.synchroniserUpload(requeteStatuts(List.of(item)), agent);

        assertThat(reponse.nbStatutsReussis()).isEqualTo(1);
        assertThat(reponse.nbStatutsConflits()).isEqualTo(0);
        assertThat(reponse.statutResultats().get(0).succes()).isTrue();
        assertThat(reponse.statutResultats().get(0).statutApplique()).isEqualTo(StatutColis.EN_TRANSIT);
        verify(notificationService).notifierChangementStatut(colisSauvegarde, StatutColis.ENREGISTRE);
    }

    @Test
    void doit_resoudre_colis_par_local_id_quand_pas_encore_synchronise_serveur() {
        when(colisRepository.findByCreeParAndLocalIdAndSupprimeFalse(agent, 101L))
                .thenReturn(Optional.of(colisSauvegarde));
        when(colisRepository.save(any(Colis.class))).thenReturn(colisSauvegarde);

        MiseAJourStatutSyncItem item = new MiseAJourStatutSyncItem(
                null, 101L, StatutColis.EN_TRANSIT, null, 0, LocalDateTime.now());

        SyncUploadReponse reponse = syncService.synchroniserUpload(requeteStatuts(List.of(item)), agent);

        assertThat(reponse.statutResultats().get(0).succes()).isTrue();
        verify(colisRepository).findByCreeParAndLocalIdAndSupprimeFalse(agent, 101L);
    }

    @Test
    void doit_rejeter_transition_illegale() {
        Colis colisRetire = Colis.builder()
                .codeTracking("TRA-2026-ABC123")
                .agenceOrigine(agenceKinshasa)
                .agenceRetrait(agenceGoma)
                .creePar(agent)
                .statutActuel(StatutColis.RETIRE)
                .version(0)
                .build();
        colisRetire.setId(10L);

        when(colisRepository.findById(10L)).thenReturn(Optional.of(colisRetire));

        MiseAJourStatutSyncItem item = new MiseAJourStatutSyncItem(
                10L, null, StatutColis.EN_TRANSIT, null, 0, LocalDateTime.now());

        SyncUploadReponse reponse = syncService.synchroniserUpload(requeteStatuts(List.of(item)), agent);

        assertThat(reponse.nbStatutsEchecs()).isEqualTo(1);
        assertThat(reponse.statutResultats().get(0).succes()).isFalse();
        assertThat(reponse.statutResultats().get(0).conflit()).isFalse();
        assertThat(reponse.statutResultats().get(0).erreur()).isNotBlank();
        verify(colisRepository, never()).save(any(Colis.class));
    }

    // --- conflit de version (Last-Write-Wins) ---

    @Test
    void doit_ignorer_changement_client_quand_serveur_plus_recent() {
        Colis colisVersion2 = Colis.builder()
                .codeTracking("TRA-2026-ABC123")
                .agenceOrigine(agenceKinshasa)
                .agenceRetrait(agenceGoma)
                .creePar(agent)
                .statutActuel(StatutColis.EN_TRANSIT)
                .version(2)
                .build();
        colisVersion2.setId(10L);

        MiseAJourStatut dernierChangementServeur = MiseAJourStatut.builder()
                .colis(colisVersion2)
                .statut(StatutColis.EN_TRANSIT)
                .build();
        dernierChangementServeur.setDateCreation(LocalDateTime.now());

        when(colisRepository.findById(10L)).thenReturn(Optional.of(colisVersion2));
        when(miseAJourStatutRepository.findByColisOrderByDateCreationDesc(colisVersion2))
                .thenReturn(List.of(dernierChangementServeur));

        MiseAJourStatutSyncItem item = new MiseAJourStatutSyncItem(
                10L, null, StatutColis.ARRIVE_AGENCE, null, 0,
                LocalDateTime.now().minusMinutes(10));

        SyncUploadReponse reponse = syncService.synchroniserUpload(requeteStatuts(List.of(item)), agent);

        assertThat(reponse.nbStatutsConflits()).isEqualTo(1);
        assertThat(reponse.statutResultats().get(0).succes()).isFalse();
        assertThat(reponse.statutResultats().get(0).conflit()).isTrue();
        verify(colisRepository, never()).save(any(Colis.class));
    }

    @Test
    void doit_appliquer_changement_client_malgre_conflit_version_quand_plus_recent() {
        Colis colisVersion2 = Colis.builder()
                .codeTracking("TRA-2026-ABC123")
                .agenceOrigine(agenceKinshasa)
                .agenceRetrait(agenceGoma)
                .creePar(agent)
                .statutActuel(StatutColis.EN_TRANSIT)
                .version(2)
                .build();
        colisVersion2.setId(10L);

        MiseAJourStatut dernierChangementServeur = MiseAJourStatut.builder()
                .colis(colisVersion2)
                .statut(StatutColis.EN_TRANSIT)
                .build();
        dernierChangementServeur.setDateCreation(LocalDateTime.now().minusHours(1));

        when(colisRepository.findById(10L)).thenReturn(Optional.of(colisVersion2));
        when(colisRepository.save(any(Colis.class))).thenReturn(colisVersion2);
        when(miseAJourStatutRepository.findByColisOrderByDateCreationDesc(colisVersion2))
                .thenReturn(List.of(dernierChangementServeur));

        MiseAJourStatutSyncItem item = new MiseAJourStatutSyncItem(
                10L, null, StatutColis.ARRIVE_AGENCE, null, 0, LocalDateTime.now());

        SyncUploadReponse reponse = syncService.synchroniserUpload(requeteStatuts(List.of(item)), agent);

        assertThat(reponse.nbStatutsReussis()).isEqualTo(1);
        assertThat(reponse.statutResultats().get(0).conflit()).isFalse();
        verify(colisRepository).save(any(Colis.class));
    }

    // --- download ---

    @Test
    void doit_retourner_tous_les_colis_modifies_pour_un_admin() {
        LocalDateTime depuis = LocalDateTime.now().minusHours(1);
        List<Colis> colisModifies = List.of(colisSauvegarde);
        List<ColisReponse> reponsesAttendues = List.of(colisReponseMinimale());

        when(colisRepository.trouverModifiesDepuis(depuis)).thenReturn(colisModifies);
        when(colisMapper.versReponses(colisModifies)).thenReturn(reponsesAttendues);

        SyncDownloadReponse reponse = syncService.synchroniserDownload(depuis, admin);

        assertThat(reponse.colis()).isEqualTo(reponsesAttendues);
        assertThat(reponse.curseur()).isNotNull();
        verify(colisRepository).trouverModifiesDepuis(depuis);
        verify(colisRepository, never()).trouverModifiesDepuisParAgence(any(), any());
    }

    @Test
    void doit_filtrer_par_agence_pour_un_non_admin() {
        LocalDateTime depuis = LocalDateTime.now().minusHours(1);
        List<Colis> colisModifies = List.of(colisSauvegarde);
        List<ColisReponse> reponsesAttendues = List.of(colisReponseMinimale());

        when(colisRepository.trouverModifiesDepuisParAgence(agenceKinshasa, depuis)).thenReturn(colisModifies);
        when(colisMapper.versReponses(colisModifies)).thenReturn(reponsesAttendues);

        SyncDownloadReponse reponse = syncService.synchroniserDownload(depuis, agent);

        assertThat(reponse.colis()).isEqualTo(reponsesAttendues);
        verify(colisRepository).trouverModifiesDepuisParAgence(agenceKinshasa, depuis);
        verify(colisRepository, never()).trouverModifiesDepuis(any());
    }

    private ColisReponse colisReponseMinimale() {
        return new ColisReponse(
                10L, "uuid-test", "TRA-2026-ABC123",
                1L, "Agence Kinshasa", 2L, "Agence Goma",
                "Jean Dupont", null, null,
                "Marie Martin", null, null,
                null, null, null, null,
                StatutColis.ENREGISTRE, 101L, 0,
                LocalDateTime.now(), List.of());
    }
}
