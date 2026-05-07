package com.transitea.util;

import com.transitea.entity.enums.StatutColis;
import com.transitea.exception.TransitionStatutInvalideException;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public final class ValidateurTransitionStatut {

    private static final Map<StatutColis, Set<StatutColis>> TRANSITIONS_AUTORISEES =
            new EnumMap<>(StatutColis.class);

    static {
        TRANSITIONS_AUTORISEES.put(
                StatutColis.ENREGISTRE,
                EnumSet.of(StatutColis.PRIS_EN_CHARGE, StatutColis.REFUSE)
        );
        TRANSITIONS_AUTORISEES.put(
                StatutColis.PRIS_EN_CHARGE,
                EnumSet.of(StatutColis.EN_TRANSIT, StatutColis.REFUSE)
        );
        TRANSITIONS_AUTORISEES.put(
                StatutColis.EN_TRANSIT,
                EnumSet.of(StatutColis.PASSAGE_FRONTIERE, StatutColis.ARRIVE_DEPOT, StatutColis.EN_DOUANE)
        );
        TRANSITIONS_AUTORISEES.put(
                StatutColis.PASSAGE_FRONTIERE,
                EnumSet.of(StatutColis.EN_DOUANE, StatutColis.EN_TRANSIT)
        );
        TRANSITIONS_AUTORISEES.put(
                StatutColis.EN_DOUANE,
                EnumSet.of(StatutColis.EN_TRANSIT, StatutColis.ARRIVE_DEPOT)
        );
        TRANSITIONS_AUTORISEES.put(
                StatutColis.ARRIVE_DEPOT,
                EnumSet.of(StatutColis.EN_LIVRAISON)
        );
        TRANSITIONS_AUTORISEES.put(
                StatutColis.EN_LIVRAISON,
                EnumSet.of(StatutColis.LIVRE, StatutColis.RETOUR_EXPEDITEUR)
        );
        TRANSITIONS_AUTORISEES.put(
                StatutColis.REFUSE,
                EnumSet.of(StatutColis.RETOUR_EXPEDITEUR)
        );
        TRANSITIONS_AUTORISEES.put(StatutColis.LIVRE, EnumSet.noneOf(StatutColis.class));
        TRANSITIONS_AUTORISEES.put(StatutColis.RETOUR_EXPEDITEUR, EnumSet.noneOf(StatutColis.class));
    }

    private ValidateurTransitionStatut() {
    }

    public static void valider(StatutColis ancien, StatutColis nouveau) {
        Set<StatutColis> statutsAutorisees = TRANSITIONS_AUTORISEES.getOrDefault(
                ancien, EnumSet.noneOf(StatutColis.class));

        if (!statutsAutorisees.contains(nouveau)) {
            throw new TransitionStatutInvalideException(ancien, nouveau);
        }
    }

    public static boolean estStatutTerminal(StatutColis statut) {
        Set<StatutColis> suivants = TRANSITIONS_AUTORISEES.getOrDefault(
                statut, EnumSet.noneOf(StatutColis.class));
        return suivants.isEmpty();
    }
}
