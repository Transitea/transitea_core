package com.transitea.util;

import com.transitea.entity.enums.StatutColis;
import com.transitea.exception.TransitionStatutInvalideException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;

class ValidateurTransitionStatutTest {

    @ParameterizedTest(name = "{0} -> {1} doit etre valide")
    @CsvSource({
        "ENREGISTRE, PRIS_EN_CHARGE",
        "ENREGISTRE, REFUSE",
        "PRIS_EN_CHARGE, EN_TRANSIT",
        "PRIS_EN_CHARGE, REFUSE",
        "EN_TRANSIT, ARRIVE_DEPOT",
        "EN_TRANSIT, PASSAGE_FRONTIERE",
        "EN_TRANSIT, EN_DOUANE",
        "PASSAGE_FRONTIERE, EN_DOUANE",
        "PASSAGE_FRONTIERE, EN_TRANSIT",
        "EN_DOUANE, EN_TRANSIT",
        "EN_DOUANE, ARRIVE_DEPOT",
        "ARRIVE_DEPOT, EN_LIVRAISON",
        "EN_LIVRAISON, LIVRE",
        "EN_LIVRAISON, RETOUR_EXPEDITEUR",
        "REFUSE, RETOUR_EXPEDITEUR"
    })
    void doit_accepter_les_transitions_autorisees(StatutColis ancien, StatutColis nouveau) {
        assertThatCode(() -> ValidateurTransitionStatut.valider(ancien, nouveau))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "{0} -> {1} doit etre invalide")
    @CsvSource({
        "ENREGISTRE, LIVRE",
        "ENREGISTRE, EN_TRANSIT",
        "PRIS_EN_CHARGE, ENREGISTRE",
        "EN_TRANSIT, ENREGISTRE",
        "ARRIVE_DEPOT, ENREGISTRE",
        "LIVRE, EN_LIVRAISON",
        "RETOUR_EXPEDITEUR, ENREGISTRE"
    })
    void doit_lancer_exception_pour_les_transitions_interdites(StatutColis ancien, StatutColis nouveau) {
        assertThatThrownBy(() -> ValidateurTransitionStatut.valider(ancien, nouveau))
                .isInstanceOf(TransitionStatutInvalideException.class);
    }

    @Test
    void doit_retourner_true_pour_statut_livre() {
        assertThat(ValidateurTransitionStatut.estStatutTerminal(StatutColis.LIVRE)).isTrue();
    }

    @Test
    void doit_retourner_true_pour_statut_retour_expediteur() {
        assertThat(ValidateurTransitionStatut.estStatutTerminal(StatutColis.RETOUR_EXPEDITEUR)).isTrue();
    }

    @Test
    void doit_retourner_false_pour_statut_enregistre() {
        assertThat(ValidateurTransitionStatut.estStatutTerminal(StatutColis.ENREGISTRE)).isFalse();
    }

    @Test
    void doit_retourner_false_pour_statut_en_transit() {
        assertThat(ValidateurTransitionStatut.estStatutTerminal(StatutColis.EN_TRANSIT)).isFalse();
    }
}
