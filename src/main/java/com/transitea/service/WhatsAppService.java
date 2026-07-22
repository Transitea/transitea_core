package com.transitea.service;

public interface WhatsAppService {

    /**
     * Envoie un message WhatsApp via l'API Graph (Meta).
     *
     * @return true si le message a ete accepte par l'API, false sinon
     * (echec reseau, API non configuree, reponse en erreur).
     */
    boolean envoyerMessage(String telephoneDestinataire, String message);

    /**
     * Envoie une image WhatsApp (ex. QR code de retrait) avec une legende
     * optionnelle, via l'API Graph (Meta).
     *
     * @return true si le message a ete accepte par l'API, false sinon
     * (echec reseau, API non configuree, reponse en erreur, upload media echoue).
     */
    boolean envoyerImage(String telephoneDestinataire, byte[] image, String legende);
}
