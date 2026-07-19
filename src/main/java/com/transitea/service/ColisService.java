package com.transitea.service;

import com.transitea.dto.request.CreationColisRequete;
import com.transitea.dto.request.MiseAJourStatutRequete;
import com.transitea.dto.response.ColisReponse;
import com.transitea.dto.response.ReponsePagee;
import com.transitea.dto.response.StatistiquesReponse;
import com.transitea.entity.Utilisateur;
import com.transitea.entity.enums.StatutColis;
import org.springframework.data.domain.Pageable;

public interface ColisService {

    ColisReponse creer(CreationColisRequete requete, Utilisateur creePar);

    ReponsePagee<ColisReponse> lister(Utilisateur utilisateur, StatutColis statut, Pageable pageable);

    ReponsePagee<ColisReponse> rechercher(Utilisateur utilisateur, String recherche, Pageable pageable);

    ColisReponse trouverParId(Long id, Utilisateur utilisateur);

    ColisReponse mettreAJourStatut(Long id, MiseAJourStatutRequete requete, Utilisateur utilisateur);

    ColisReponse retirer(String codeTracking, Utilisateur utilisateur);

    void supprimer(Long id, Utilisateur utilisateur);

    byte[] genererQrCode(Long id, Utilisateur utilisateur);

    StatistiquesReponse obtenirStatistiques(Utilisateur utilisateur);
}
