package com.transitea.service;

import com.transitea.entity.Colis;
import com.transitea.entity.enums.StatutColis;

public interface NotificationService {

    void notifierChangementStatut(Colis colis, StatutColis ancienStatut);
}
