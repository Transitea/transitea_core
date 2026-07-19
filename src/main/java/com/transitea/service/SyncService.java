package com.transitea.service;

import com.transitea.dto.request.SyncRequete;
import com.transitea.dto.response.SyncReponse;
import com.transitea.entity.Utilisateur;

public interface SyncService {

    SyncReponse synchroniser(SyncRequete requete, Utilisateur transporteur);
}
