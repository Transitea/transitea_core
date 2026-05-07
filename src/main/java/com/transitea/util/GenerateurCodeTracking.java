package com.transitea.util;

import java.security.SecureRandom;
import java.time.LocalDate;

public final class GenerateurCodeTracking {

    private static final SecureRandom ALEATOIRE = new SecureRandom();
    private static final String CARACTERES_AUTORISES = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int LONGUEUR_SUFFIXE = 6;

    private GenerateurCodeTracking() {
    }

    public static String generer() {
        int annee = LocalDate.now().getYear();
        String suffixe = genererSuffixeAleatoire();
        return String.format("TRA-%d-%s", annee, suffixe);
    }

    private static String genererSuffixeAleatoire() {
        StringBuilder suffixe = new StringBuilder(LONGUEUR_SUFFIXE);
        for (int i = 0; i < LONGUEUR_SUFFIXE; i++) {
            int index = ALEATOIRE.nextInt(CARACTERES_AUTORISES.length());
            suffixe.append(CARACTERES_AUTORISES.charAt(index));
        }
        return suffixe.toString();
    }
}
