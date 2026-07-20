package com.transitea.service.impl;

import com.transitea.entity.Agence;
import com.transitea.entity.Enseigne;
import com.transitea.repository.EnseigneRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuotaServiceImplTest {

    @Mock
    private EnseigneRepository enseigneRepository;

    @InjectMocks
    private QuotaServiceImpl quotaService;

    private Enseigne enseigne;
    private Agence agence;

    @BeforeEach
    void initialiser() {
        enseigne = Enseigne.builder()
                .nom("Transitea France")
                .quotaColisMois(100)
                .colisMoisCourant(0)
                .build();
        enseigne.setId(1L);

        agence = Agence.builder().nom("Agence Paris").ville("Paris").enseigne(enseigne).build();
        agence.setId(1L);
    }

    @Test
    void doit_incrementer_le_compteur_mensuel() {
        when(enseigneRepository.save(enseigne)).thenReturn(enseigne);

        quotaService.enregistrerColis(agence);

        ArgumentCaptor<Enseigne> captor = ArgumentCaptor.forClass(Enseigne.class);
        verify(enseigneRepository).save(captor.capture());
        assertThat(captor.getValue().getColisMoisCourant()).isEqualTo(1);
    }

    @Test
    void ne_doit_pas_planter_quand_quota_non_defini() {
        enseigne.setQuotaColisMois(null);
        when(enseigneRepository.save(enseigne)).thenReturn(enseigne);

        quotaService.enregistrerColis(agence);

        assertThat(enseigne.getColisMoisCourant()).isEqualTo(1);
    }

    @Test
    void doit_incrementer_correctement_en_franchissant_le_seuil_80_pourcent() {
        enseigne.setColisMoisCourant(79);
        when(enseigneRepository.save(enseigne)).thenReturn(enseigne);

        quotaService.enregistrerColis(agence);

        assertThat(enseigne.getColisMoisCourant()).isEqualTo(80);
    }

    @Test
    void doit_incrementer_correctement_en_franchissant_le_quota_complet() {
        enseigne.setColisMoisCourant(99);
        when(enseigneRepository.save(enseigne)).thenReturn(enseigne);

        quotaService.enregistrerColis(agence);

        assertThat(enseigne.getColisMoisCourant()).isEqualTo(100);
    }
}
