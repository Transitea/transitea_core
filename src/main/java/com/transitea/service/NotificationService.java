package com.transitea.service;

import com.transitea.dto.response.NotificationReponse;
import com.transitea.dto.response.ReponsePagee;
import com.transitea.entity.Colis;
import com.transitea.entity.Utilisateur;
import com.transitea.entity.enums.StatutColis;
import org.springframework.data.domain.Pageable;

public interface NotificationService {

    void notifierChangementStatut(Colis colis, StatutColis ancienStatut);

    ReponsePagee<NotificationReponse> lister(Utilisateur utilisateur, Pageable pageable);
}
