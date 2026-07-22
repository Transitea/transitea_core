package com.transitea.service.impl;

import com.transitea.dto.response.ClientReponse;
import com.transitea.dto.response.ReponsePagee;
import com.transitea.entity.Agence;
import com.transitea.entity.Utilisateur;
import com.transitea.entity.enums.Role;
import com.transitea.exception.AccesNonAutoriseException;
import com.transitea.repository.ColisRepository;
import com.transitea.service.ClientService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ClientServiceImpl implements ClientService {

    private final ColisRepository colisRepository;

    public ClientServiceImpl(ColisRepository colisRepository) {
        this.colisRepository = colisRepository;
    }

    @Override
    public ReponsePagee<ClientReponse> lister(Utilisateur utilisateur, Pageable pageable) {
        Page<ClientReponse> page = utilisateur.getRole() == Role.ADMIN
                ? colisRepository.agregerClients(pageable)
                : colisRepository.agregerClientsParAgence(agenceDeLUtilisateur(utilisateur), pageable);

        return ReponsePagee.depuis(page);
    }

    private Agence agenceDeLUtilisateur(Utilisateur utilisateur) {
        if (utilisateur.getAgence() == null) {
            throw new AccesNonAutoriseException();
        }
        return utilisateur.getAgence();
    }
}
