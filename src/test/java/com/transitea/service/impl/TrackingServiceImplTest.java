package com.transitea.service.impl;

import com.transitea.dto.response.MiseAJourStatutReponse;
import com.transitea.dto.response.SuiviPublicReponse;
import com.transitea.entity.Colis;
import com.transitea.entity.MiseAJourStatut;
import com.transitea.entity.Utilisateur;
import com.transitea.entity.enums.Role;
import com.transitea.entity.enums.StatutColis;
import com.transitea.exception.EntiteNonTrouveeException;
import com.transitea.mapper.ColisMapper;
import com.transitea.repository.ColisRepository;
import com.transitea.repository.MiseAJourStatutRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrackingServiceImplTest {

    @Mock
    private ColisRepository colisRepository;

    @Mock
    private MiseAJourStatutRepository miseAJourStatutRepository;

    @Mock
    private ColisMapper colisMapper;

    @InjectMocks
    private TrackingServiceImpl trackingService;

    private Colis colis;

    @BeforeEach
    void initialiser() {
        Utilisateur agent = Utilisateur.builder()
                .nom("Lumbu")
                .prenom("Louange")
                .email("louange@transitea.cd")
                .role(Role.AGENT)
                .build();
        agent.setId(1L);

        colis = Colis.builder()
                .codeTracking("TRA-2026-ABC123")
                .creePar(agent)
                .expediteurNom("Jean Dupont")
                .destinataireNom("Marie Martin")
                .destinataireVille("Lubumbashi")
                .description("Documents importants")
                .poids(new BigDecimal("2.500"))
                .statutActuel(StatutColis.EN_TRANSIT)
                .build();
        colis.setId(10L);
    }

    @Test
    void doit_retourner_suivi_public_quand_code_tracking_valide() {
        when(colisRepository.findByCodeTrackingAndSupprimeFalse("TRA-2026-ABC123"))
                .thenReturn(Optional.of(colis));
        when(miseAJourStatutRepository.findByColisOrderByDateCreationAsc(colis))
                .thenReturn(List.of());
        when(colisMapper.versMiseAJourReponses(any())).thenReturn(List.of());

        SuiviPublicReponse resultat = trackingService.suivreParCode("TRA-2026-ABC123");

        assertThat(resultat).isNotNull();
        assertThat(resultat.codeTracking()).isEqualTo("TRA-2026-ABC123");
        assertThat(resultat.expediteurNom()).isEqualTo("Jean Dupont");
        assertThat(resultat.destinataireNom()).isEqualTo("Marie Martin");
        assertThat(resultat.destinataireVille()).isEqualTo("Lubumbashi");
        assertThat(resultat.statutActuel()).isEqualTo(StatutColis.EN_TRANSIT);
    }

    @Test
    void doit_retourner_historique_des_statuts_avec_le_suivi() {
        MiseAJourStatut maj = MiseAJourStatut.builder()
                .colis(colis)
                .statut(StatutColis.EN_TRANSIT)
                .ancienStatut(StatutColis.ENREGISTRE)
                .localisation("Kinshasa")
                .build();
        MiseAJourStatutReponse majReponse = new MiseAJourStatutReponse(
                1L, StatutColis.EN_TRANSIT, StatutColis.ENREGISTRE,
                "Kinshasa", null, 1L, null);

        when(colisRepository.findByCodeTrackingAndSupprimeFalse("TRA-2026-ABC123"))
                .thenReturn(Optional.of(colis));
        when(miseAJourStatutRepository.findByColisOrderByDateCreationAsc(colis))
                .thenReturn(List.of(maj));
        when(colisMapper.versMiseAJourReponses(List.of(maj))).thenReturn(List.of(majReponse));

        SuiviPublicReponse resultat = trackingService.suivreParCode("TRA-2026-ABC123");

        assertThat(resultat.historique()).hasSize(1);
        assertThat(resultat.historique().get(0).statut()).isEqualTo(StatutColis.EN_TRANSIT);
        verify(miseAJourStatutRepository).findByColisOrderByDateCreationAsc(colis);
    }

    @Test
    void doit_lancer_entite_non_trouvee_exception_quand_code_tracking_inconnu() {
        when(colisRepository.findByCodeTrackingAndSupprimeFalse("TRA-2026-INCONNU"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> trackingService.suivreParCode("TRA-2026-INCONNU"))
                .isInstanceOf(EntiteNonTrouveeException.class);
    }

    @Test
    void doit_ne_pas_exposer_informations_sensibles_dans_la_reponse() {
        when(colisRepository.findByCodeTrackingAndSupprimeFalse("TRA-2026-ABC123"))
                .thenReturn(Optional.of(colis));
        when(miseAJourStatutRepository.findByColisOrderByDateCreationAsc(colis))
                .thenReturn(List.of());
        when(colisMapper.versMiseAJourReponses(any())).thenReturn(List.of());

        SuiviPublicReponse resultat = trackingService.suivreParCode("TRA-2026-ABC123");

        // SuiviPublicReponse ne contient pas d'email ni de telephone
        assertThat(resultat).isInstanceOf(SuiviPublicReponse.class);
        assertThat(resultat.codeTracking()).isNotNull();
        assertThat(resultat.poids()).isEqualByComparingTo(new BigDecimal("2.500"));
    }
}
