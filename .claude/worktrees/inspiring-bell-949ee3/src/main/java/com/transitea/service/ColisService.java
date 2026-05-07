package com.transitea.service;

import com.transitea.dto.request.CreationColisRequete;
import com.transitea.dto.request.MiseAJourStatutRequete;
import com.transitea.dto.response.ColisReponse;
import com.transitea.dto.response.ReponsePagee;
import com.transitea.entity.Utilisateur;
import com.transitea.entity.enums.StatutColis;
import org.springframework.data.domain.Pageable;

public interface ColisService {

    ColisReponse creer(CreationColisRequete requete, Utilisateur transporteur);

    ReponsePagee<ColisReponse> lister(Utilisateur transporteur, StatutColis statut, Pageable pageable);

    ColisReponse trouverParId(Long id, Utilisateur transporteur);

    ColisReponse mettreAJourStatut(Long id, MiseAJourStatutRequete requete, Utilisateur utilisateur);

    void supprimer(Long id, Utilisateur transporteur);

    byte[] genererQrCode(Long id, Utilisateur utilisateur);
}
