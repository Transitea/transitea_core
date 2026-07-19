package com.transitea.service;

import com.transitea.dto.request.CreationAgenceRequete;
import com.transitea.dto.response.AgenceReponse;

import java.util.List;

public interface AgenceService {

    AgenceReponse creer(CreationAgenceRequete requete);

    List<AgenceReponse> lister();

    AgenceReponse trouverParId(Long id);
}
