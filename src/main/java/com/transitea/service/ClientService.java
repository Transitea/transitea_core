package com.transitea.service;

import com.transitea.dto.response.ClientReponse;
import com.transitea.dto.response.ReponsePagee;
import com.transitea.entity.Utilisateur;
import org.springframework.data.domain.Pageable;

public interface ClientService {

    /**
     * Clients (expediteurs/destinataires) agreges depuis les colis : aucune
     * entite Client dediee n'existe dans le modele de donnees (CDC section 5).
     */
    ReponsePagee<ClientReponse> lister(Utilisateur utilisateur, Pageable pageable);
}
