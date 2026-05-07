package com.transitea.exception;

public class ErreurMetier extends RuntimeException {

    public ErreurMetier(String message) {
        super(message);
    }

    public ErreurMetier(String message, Throwable cause) {
        super(message, cause);
    }
}
