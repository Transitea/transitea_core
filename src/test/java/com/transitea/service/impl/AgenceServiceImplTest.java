package com.transitea.service.impl;

import com.transitea.dto.request.CreationAgenceRequete;
import com.transitea.dto.response.AgenceReponse;
import com.transitea.entity.Agence;
import com.transitea.entity.Enseigne;
import com.transitea.exception.EntiteNonTrouveeException;
import com.transitea.exception.ErreurMetier;
import com.transitea.repository.AgenceRepository;
import com.transitea.repository.EnseigneRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgenceServiceImplTest {

    @Mock
    private AgenceRepository agenceRepository;

    @Mock
    private EnseigneRepository enseigneRepository;

    @InjectMocks
    private AgenceServiceImpl agenceService;

    private Enseigne enseigne;
    private Agence agence;

    @BeforeEach
    void initialiser() {
        enseigne = Enseigne.builder().nom("Transitea France").build();
        enseigne.setId(1L);

        agence = Agence.builder().nom("Agence Paris").ville("Paris")
                .adresse("12 Rue de Rivoli, 75004 Paris").enseigne(enseigne).build();
        agence.setId(1L);
    }

    @Test
    void doit_creer_une_agence_rattachee_a_lenseigne() {
        CreationAgenceRequete requete = new CreationAgenceRequete(
                "Agence Nice", "Nice", "1 Promenade des Anglais");

        when(enseigneRepository.findAll()).thenReturn(List.of(enseigne));
        when(agenceRepository.save(any(Agence.class))).thenReturn(agence);

        AgenceReponse resultat = agenceService.creer(requete);

        assertThat(resultat).isNotNull();
        ArgumentCaptor<Agence> captor = ArgumentCaptor.forClass(Agence.class);
        verify(agenceRepository).save(captor.capture());
        assertThat(captor.getValue().getEnseigne()).isEqualTo(enseigne);
    }

    @Test
    void doit_lancer_erreur_metier_quand_aucune_enseigne_configuree() {
        CreationAgenceRequete requete = new CreationAgenceRequete(
                "Agence Nice", "Nice", "1 Promenade des Anglais");

        when(enseigneRepository.findAll()).thenReturn(List.of());

        assertThatThrownBy(() -> agenceService.creer(requete))
                .isInstanceOf(ErreurMetier.class);

        verify(agenceRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void doit_lister_les_agences_non_supprimees() {
        when(agenceRepository.findBySupprimeFalse()).thenReturn(List.of(agence));

        List<AgenceReponse> resultat = agenceService.lister();

        assertThat(resultat).hasSize(1);
        assertThat(resultat.get(0).nom()).isEqualTo("Agence Paris");
    }

    @Test
    void doit_retourner_le_detail_dune_agence() {
        when(agenceRepository.findByIdAndSupprimeFalse(1L)).thenReturn(Optional.of(agence));

        AgenceReponse resultat = agenceService.trouverParId(1L);

        assertThat(resultat.ville()).isEqualTo("Paris");
        assertThat(resultat.enseigneId()).isEqualTo(1L);
    }

    @Test
    void doit_lancer_exception_quand_agence_inexistante() {
        when(agenceRepository.findByIdAndSupprimeFalse(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> agenceService.trouverParId(99L))
                .isInstanceOf(EntiteNonTrouveeException.class);
    }
}
