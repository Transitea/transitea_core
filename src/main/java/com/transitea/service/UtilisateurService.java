package com.transitea.service;

import com.transitea.dto.request.CreationUtilisateurRequete;
import com.transitea.dto.response.ReponsePagee;
import com.transitea.dto.response.UtilisateurReponse;
import com.transitea.entity.enums.StatutUtilisateur;
import org.springframework.data.domain.Pageable;

public interface UtilisateurService {

    ReponsePagee<UtilisateurReponse> lister(Long agenceId, Pageable pageable);

    UtilisateurReponse creer(CreationUtilisateurRequete requete);

    UtilisateurReponse mettreAJourStatut(Long id, StatutUtilisateur statut);
}
