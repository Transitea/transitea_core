package com.transitea.exception;

import com.transitea.entity.enums.StatutColis;

public class TransitionStatutInvalideException extends ErreurMetier {

    public TransitionStatutInvalideException(StatutColis ancien, StatutColis nouveau) {
        super(String.format(
                "Transition de statut invalide : %s -> %s",
                ancien.name(),
                nouveau.name()
        ));
    }
}
