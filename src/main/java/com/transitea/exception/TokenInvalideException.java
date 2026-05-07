package com.transitea.exception;

public class TokenInvalideException extends ErreurMetier {

    public TokenInvalideException() {
        super("Token invalide ou expire");
    }

    public TokenInvalideException(String message) {
        super(message);
    }
}
