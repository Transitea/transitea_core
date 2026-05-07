package com.transitea.service;

import com.transitea.dto.request.ConnexionRequete;
import com.transitea.dto.request.InscriptionRequete;
import com.transitea.dto.request.RafraichissementRequete;
import com.transitea.dto.response.AuthReponse;
import com.transitea.dto.response.UtilisateurReponse;
import com.transitea.entity.Utilisateur;

public interface AuthService {

    AuthReponse inscrire(InscriptionRequete requete);

    AuthReponse connecter(ConnexionRequete requete);

    AuthReponse rafraichir(RafraichissementRequete requete);

    void deconnecter(String refreshToken);

    UtilisateurReponse obtenirProfil(Utilisateur utilisateur);
}
