package com.transitea.service.impl;

import com.transitea.entity.Colis;
import com.transitea.entity.Utilisateur;
import com.transitea.entity.enums.Role;
import com.transitea.entity.enums.StatutColis;
import com.transitea.repository.ColisRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExportServiceImplTest {

    @Mock
    private ColisRepository colisRepository;

    @InjectMocks
    private ExportServiceImpl exportService;

    private Utilisateur transporteur;
    private Utilisateur admin;
    private Utilisateur operateur;
    private Colis colis;
    private LocalDateTime debut;
    private LocalDateTime fin;

    @BeforeEach
    void initialiser() {
        transporteur = Utilisateur.builder()
                .nom("Lumbu")
                .prenom("Louange")
                .email("louange@transitea.cd")
                .role(Role.TRANSPORTEUR)
                .build();
        transporteur.setId(1L);

        admin = Utilisateur.builder()
                .nom("Admin")
                .prenom("System")
                .email("admin@transitea.cd")
                .role(Role.ADMIN)
                .build();
        admin.setId(2L);

        operateur = Utilisateur.builder()
                .nom("Mutombo")
                .prenom("Pierre")
                .email("operateur@transitea.cd")
                .role(Role.OPERATEUR)
                .build();
        operateur.setId(3L);

        debut = LocalDateTime.of(2026, 1, 1, 0, 0);
        fin = LocalDateTime.of(2026, 12, 31, 23, 59);

        colis = Colis.builder()
                .codeTracking("TRA-2026-ABC123")
                .transporteur(transporteur)
                .expediteurNom("Jean Dupont")
                .expediteurTelephone("0812345678")
                .destinataireNom("Marie Martin")
                .destinataireTelephone("0812345679")
                .destinataireVille("Kinshasa")
                .description("Documents")
                .poids(new BigDecimal("2.500"))
                .statutActuel(StatutColis.LIVRE)
                .build();
        colis.setId(10L);
        colis.setDateCreation(LocalDateTime.of(2026, 6, 15, 10, 0));
    }

    @Test
    void doit_utiliser_repository_transporteur_quand_role_transporteur() {
        when(colisRepository.findByTransporteurAndDateCreationBetweenAndSupprimeFalse(
                transporteur, debut, fin))
                .thenReturn(List.of(colis));

        byte[] resultat = exportService.exporterCsvColis(transporteur, debut, fin);

        assertThat(resultat).isNotEmpty();
        verify(colisRepository).findByTransporteurAndDateCreationBetweenAndSupprimeFalse(
                transporteur, debut, fin);
    }

    @Test
    void doit_utiliser_findAll_quand_role_admin() {
        when(colisRepository.findBySupprimeFalse(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(colis)));

        exportService.exporterCsvColis(admin, debut, fin);

        verify(colisRepository).findBySupprimeFalse(any(Pageable.class));
    }

    @Test
    void doit_utiliser_findAll_quand_role_operateur() {
        when(colisRepository.findBySupprimeFalse(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(colis)));

        exportService.exporterCsvColis(operateur, debut, fin);

        verify(colisRepository).findBySupprimeFalse(any(Pageable.class));
    }

    @Test
    void doit_contenir_en_tete_csv() {
        when(colisRepository.findByTransporteurAndDateCreationBetweenAndSupprimeFalse(
                any(), any(), any()))
                .thenReturn(List.of());

        byte[] resultat = exportService.exporterCsvColis(transporteur, debut, fin);

        String csv = new String(resultat, StandardCharsets.UTF_8);
        assertThat(csv).startsWith("Code Tracking,");
        assertThat(csv).contains("Expediteur");
        assertThat(csv).contains("Destinataire");
        assertThat(csv).contains("Statut");
    }

    @Test
    void doit_contenir_donnees_colis_dans_csv() {
        when(colisRepository.findByTransporteurAndDateCreationBetweenAndSupprimeFalse(
                any(), any(), any()))
                .thenReturn(List.of(colis));

        byte[] resultat = exportService.exporterCsvColis(transporteur, debut, fin);

        String csv = new String(resultat, StandardCharsets.UTF_8);
        assertThat(csv).contains("TRA-2026-ABC123");
        assertThat(csv).contains("Jean Dupont");
        assertThat(csv).contains("Marie Martin");
        assertThat(csv).contains("Kinshasa");
        assertThat(csv).contains("LIVRE");
        assertThat(csv).contains("2.500");
    }

    @Test
    void doit_echapper_virgules_dans_les_champs() {
        colis = Colis.builder()
                .codeTracking("TRA-2026-ABC999")
                .transporteur(transporteur)
                .expediteurNom("Dupont, Jean")
                .destinataireNom("Martin")
                .poids(new BigDecimal("1.000"))
                .statutActuel(StatutColis.ENREGISTRE)
                .build();
        colis.setId(11L);
        colis.setDateCreation(LocalDateTime.of(2026, 6, 15, 10, 0));

        when(colisRepository.findByTransporteurAndDateCreationBetweenAndSupprimeFalse(
                any(), any(), any()))
                .thenReturn(List.of(colis));

        byte[] resultat = exportService.exporterCsvColis(transporteur, debut, fin);

        String csv = new String(resultat, StandardCharsets.UTF_8);
        assertThat(csv).contains("\"Dupont, Jean\"");
    }

    @Test
    void doit_retourner_csv_vide_quand_aucun_colis() {
        when(colisRepository.findByTransporteurAndDateCreationBetweenAndSupprimeFalse(
                any(), any(), any()))
                .thenReturn(List.of());

        byte[] resultat = exportService.exporterCsvColis(transporteur, debut, fin);

        String csv = new String(resultat, StandardCharsets.UTF_8);
        String[] lignes = csv.split("\n");
        assertThat(lignes).hasSize(1);
    }
}
