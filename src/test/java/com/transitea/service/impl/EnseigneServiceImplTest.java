package com.transitea.service.impl;

import com.transitea.dto.response.EnseigneReponse;
import com.transitea.entity.Enseigne;
import com.transitea.entity.enums.PalierAbonnement;
import com.transitea.entity.enums.StatutEnseigne;
import com.transitea.exception.ErreurMetier;
import com.transitea.repository.EnseigneRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnseigneServiceImplTest {

    @Mock
    private EnseigneRepository enseigneRepository;

    @InjectMocks
    private EnseigneServiceImpl enseigneService;

    @Test
    void doit_retourner_le_pourcentage_de_consommation() {
        Enseigne enseigne = Enseigne.builder()
                .nom("Transitea France")
                .palierAbonnement(PalierAbonnement.STANDARD)
                .quotaColisMois(100)
                .colisMoisCourant(42)
                .statut(StatutEnseigne.ACTIF)
                .build();
        enseigne.setId(1L);
        when(enseigneRepository.findAll()).thenReturn(List.of(enseigne));

        EnseigneReponse resultat = enseigneService.obtenir();

        assertThat(resultat.nom()).isEqualTo("Transitea France");
        assertThat(resultat.pourcentageConsomme()).isEqualTo(42.0);
    }

    @Test
    void doit_retourner_zero_pourcent_quand_quota_non_defini() {
        Enseigne enseigne = Enseigne.builder()
                .nom("Transitea France")
                .colisMoisCourant(5)
                .statut(StatutEnseigne.ACTIF)
                .build();
        enseigne.setId(1L);
        when(enseigneRepository.findAll()).thenReturn(List.of(enseigne));

        EnseigneReponse resultat = enseigneService.obtenir();

        assertThat(resultat.pourcentageConsomme()).isEqualTo(0.0);
    }

    @Test
    void doit_lancer_erreur_metier_quand_aucune_enseigne() {
        when(enseigneRepository.findAll()).thenReturn(List.of());

        assertThatThrownBy(() -> enseigneService.obtenir())
                .isInstanceOf(ErreurMetier.class);
    }
}
