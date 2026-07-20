package com.transitea.job;

import com.transitea.entity.Enseigne;
import com.transitea.repository.EnseigneRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobExecutionContext;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReinitialisationQuotaMensuelJobTest {

    @Mock
    private EnseigneRepository enseigneRepository;

    @Mock
    private JobExecutionContext jobExecutionContext;

    @InjectMocks
    private ReinitialisationQuotaMensuelJob job;

    private Enseigne enseigne;

    @BeforeEach
    void initialiser() {
        enseigne = Enseigne.builder()
                .nom("Transitea France")
                .quotaColisMois(100)
                .colisMoisCourant(87)
                .build();
        enseigne.setId(1L);
    }

    @Test
    void doit_remettre_a_zero_le_compteur_de_toutes_les_enseignes() {
        when(enseigneRepository.findAll()).thenReturn(List.of(enseigne));
        when(enseigneRepository.saveAll(anyList())).thenReturn(List.of(enseigne));

        job.execute(jobExecutionContext);

        assertThat(enseigne.getColisMoisCourant()).isEqualTo(0);
        verify(enseigneRepository).saveAll(List.of(enseigne));
    }
}
