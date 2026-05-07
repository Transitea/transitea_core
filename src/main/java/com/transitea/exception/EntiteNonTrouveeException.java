package com.transitea.exception;

public class EntiteNonTrouveeException extends ErreurMetier {

    public EntiteNonTrouveeException(String entite, Long id) {
        super(String.format("%s introuvable avec l'identifiant : %d", entite, id));
    }

    public EntiteNonTrouveeException(String entite, String valeur) {
        super(String.format("%s introuvable avec la valeur : %s", entite, valeur));
    }
}
