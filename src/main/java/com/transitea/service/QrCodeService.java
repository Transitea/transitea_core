package com.transitea.service;

public interface QrCodeService {

    /**
     * Genere le QR code pour le contenu donne. Si un cache serveur existe
     * pour la cle fournie (et n'a pas expire), il est reutilise.
     *
     * @param contenu donnee encodee dans le QR code (ex. URL de suivi)
     * @param cleCache identifiant stable utilise comme nom de fichier cache (ex. code de suivi)
     */
    byte[] generer(String contenu, String cleCache);
}
