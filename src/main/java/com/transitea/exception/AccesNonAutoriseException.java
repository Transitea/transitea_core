package com.transitea.exception;

public class AccesNonAutoriseException extends ErreurMetier {

    public AccesNonAutoriseException() {
        super("Acces refuse : vous n'etes pas autorise a effectuer cette operation");
    }

    public AccesNonAutoriseException(String message) {
        super(message);
    }
}
