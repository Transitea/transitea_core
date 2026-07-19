package com.transitea.service;

public interface WhatsAppService {

    /**
     * Envoie un message WhatsApp via l'API Graph (Meta).
     *
     * @return true si le message a ete accepte par l'API, false sinon
     * (echec reseau, API non configuree, reponse en erreur).
     */
    boolean envoyerMessage(String telephoneDestinataire, String message);
}
