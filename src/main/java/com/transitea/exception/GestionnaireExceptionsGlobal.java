package com.transitea.exception;

import com.transitea.dto.response.ErreurReponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GestionnaireExceptionsGlobal {

    private static final Logger journal = LoggerFactory.getLogger(GestionnaireExceptionsGlobal.class);

    @ExceptionHandler(EntiteNonTrouveeException.class)
    public ResponseEntity<ErreurReponse> gererEntiteNonTrouvee(
            EntiteNonTrouveeException ex, HttpServletRequest requete) {

        ErreurReponse erreur = new ErreurReponse(
                HttpStatus.NOT_FOUND.value(),
                ex.getMessage(),
                requete.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(erreur);
    }

    @ExceptionHandler(TransitionStatutInvalideException.class)
    public ResponseEntity<ErreurReponse> gererTransitionInvalide(
            TransitionStatutInvalideException ex, HttpServletRequest requete) {

        ErreurReponse erreur = new ErreurReponse(
                HttpStatus.BAD_REQUEST.value(),
                ex.getMessage(),
                requete.getRequestURI()
        );
        return ResponseEntity.badRequest().body(erreur);
    }

    @ExceptionHandler(TokenInvalideException.class)
    public ResponseEntity<ErreurReponse> gererTokenInvalide(
            TokenInvalideException ex, HttpServletRequest requete) {

        ErreurReponse erreur = new ErreurReponse(
                HttpStatus.UNAUTHORIZED.value(),
                ex.getMessage(),
                requete.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(erreur);
    }

    @ExceptionHandler(AccesNonAutoriseException.class)
    public ResponseEntity<ErreurReponse> gererAccesNonAutorise(
            AccesNonAutoriseException ex, HttpServletRequest requete) {

        ErreurReponse erreur = new ErreurReponse(
                HttpStatus.FORBIDDEN.value(),
                ex.getMessage(),
                requete.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(erreur);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErreurReponse> gererParametreManquant(
            MissingServletRequestParameterException ex, HttpServletRequest requete) {

        ErreurReponse erreur = new ErreurReponse(
                HttpStatus.BAD_REQUEST.value(),
                "Parametre obligatoire manquant : " + ex.getParameterName(),
                requete.getRequestURI()
        );
        return ResponseEntity.badRequest().body(erreur);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErreurReponse> gererTypeParametreInvalide(
            MethodArgumentTypeMismatchException ex, HttpServletRequest requete) {

        ErreurReponse erreur = new ErreurReponse(
                HttpStatus.BAD_REQUEST.value(),
                "Parametre invalide : " + ex.getName(),
                requete.getRequestURI()
        );
        return ResponseEntity.badRequest().body(erreur);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErreurReponse> gererAccesRefuseSpringSecurity(
            AccessDeniedException ex, HttpServletRequest requete) {

        ErreurReponse erreur = new ErreurReponse(
                HttpStatus.FORBIDDEN.value(),
                "Acces refuse : privileges insuffisants",
                requete.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(erreur);
    }

    @ExceptionHandler(ErreurMetier.class)
    public ResponseEntity<ErreurReponse> gererErreurMetier(
            ErreurMetier ex, HttpServletRequest requete) {

        ErreurReponse erreur = new ErreurReponse(
                HttpStatus.BAD_REQUEST.value(),
                ex.getMessage(),
                requete.getRequestURI()
        );
        return ResponseEntity.badRequest().body(erreur);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErreurReponse> gererErreurValidation(
            MethodArgumentNotValidException ex, HttpServletRequest requete) {

        Map<String, String> erreursChamps = new LinkedHashMap<>();
        for (FieldError erreurChamp : ex.getBindingResult().getFieldErrors()) {
            erreursChamps.put(erreurChamp.getField(), erreurChamp.getDefaultMessage());
        }

        ErreurReponse erreur = new ErreurReponse(
                HttpStatus.BAD_REQUEST.value(),
                "Erreur de validation des donnees",
                requete.getRequestURI(),
                erreursChamps
        );
        return ResponseEntity.badRequest().body(erreur);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErreurReponse> gererErreurInterne(
            Exception ex, HttpServletRequest requete) {

        journal.error("Erreur interne non geree : {}", ex.getMessage(), ex);

        ErreurReponse erreur = new ErreurReponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Une erreur interne est survenue",
                requete.getRequestURI()
        );
        return ResponseEntity.internalServerError().body(erreur);
    }
}
