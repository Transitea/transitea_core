package com.transitea.service.impl;

import com.transitea.dto.request.CreationUtilisateurRequete;
import com.transitea.dto.response.ReponsePagee;
import com.transitea.dto.response.UtilisateurReponse;
import com.transitea.entity.Agence;
import com.transitea.entity.Utilisateur;
import com.transitea.entity.enums.Role;
import com.transitea.entity.enums.StatutUtilisateur;
import com.transitea.exception.EntiteNonTrouveeException;
import com.transitea.exception.ErreurMetier;
import com.transitea.repository.AgenceRepository;
import com.transitea.repository.UtilisateurRepository;
import com.transitea.service.UtilisateurService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UtilisateurServiceImpl implements UtilisateurService {

    private static final Logger journal = LoggerFactory.getLogger(UtilisateurServiceImpl.class);

    private final UtilisateurRepository utilisateurRepository;
    private final AgenceRepository agenceRepository;
    private final PasswordEncoder encodeurMotDePasse;

    public UtilisateurServiceImpl(
            UtilisateurRepository utilisateurRepository,
            AgenceRepository agenceRepository,
            PasswordEncoder encodeurMotDePasse) {
        this.utilisateurRepository = utilisateurRepository;
        this.agenceRepository = agenceRepository;
        this.encodeurMotDePasse = encodeurMotDePasse;
    }

    @Override
    @Transactional(readOnly = true)
    public ReponsePagee<UtilisateurReponse> lister(Long agenceId, Pageable pageable) {
        Page<Utilisateur> page;

        if (agenceId != null) {
            Agence agence = agenceRepository.findByIdAndSupprimeFalse(agenceId)
                    .orElseThrow(() -> new EntiteNonTrouveeException("Agence", agenceId));
            page = utilisateurRepository.findByAgenceAndSupprimeFalse(agence, pageable);
        } else {
            page = utilisateurRepository.findBySupprimeFalse(pageable);
        }

        return ReponsePagee.depuis(page.map(this::versReponse));
    }

    @Override
    public UtilisateurReponse creer(CreationUtilisateurRequete requete) {
        if (requete.role() == Role.ADMIN) {
            throw new ErreurMetier("Impossible de creer un compte ADMIN via cet endpoint");
        }

        if (utilisateurRepository.existsByEmail(requete.email())) {
            throw new ErreurMetier("Un compte existe deja avec cet email");
        }

        if (requete.telephone() != null && utilisateurRepository.existsByTelephone(requete.telephone())) {
            throw new ErreurMetier("Un compte existe deja avec ce numero de telephone");
        }

        Agence agence = agenceRepository.findByIdAndSupprimeFalse(requete.agenceId())
                .orElseThrow(() -> new EntiteNonTrouveeException("Agence", requete.agenceId()));

        Utilisateur utilisateur = Utilisateur.builder()
                .nom(requete.nom())
                .prenom(requete.prenom())
                .email(requete.email())
                .telephone(requete.telephone())
                .motDePasseHash(encodeurMotDePasse.encode(requete.motDePasse()))
                .role(requete.role())
                .agence(agence)
                .build();

        Utilisateur sauvegarde = utilisateurRepository.save(utilisateur);
        journal.info("Compte {} cree par l'administrateur pour l'agence {}",
                sauvegarde.getEmail(), agence.getNom());

        return versReponse(sauvegarde);
    }

    @Override
    public UtilisateurReponse mettreAJourStatut(Long id, StatutUtilisateur statut) {
        Utilisateur utilisateur = utilisateurRepository.findByIdAndSupprimeFalse(id)
                .orElseThrow(() -> new EntiteNonTrouveeException("Utilisateur", id));

        utilisateur.setStatut(statut);
        Utilisateur miseAJour = utilisateurRepository.save(utilisateur);

        journal.info("Statut du compte {} mis a jour : {}", miseAJour.getEmail(), statut);

        return versReponse(miseAJour);
    }

    private UtilisateurReponse versReponse(Utilisateur utilisateur) {
        return new UtilisateurReponse(
                utilisateur.getId(),
                utilisateur.getUuid(),
                utilisateur.getNom(),
                utilisateur.getPrenom(),
                utilisateur.getEmail(),
                utilisateur.getTelephone(),
                utilisateur.getRole(),
                utilisateur.getStatut(),
                utilisateur.getAgence() != null ? utilisateur.getAgence().getId() : null,
                utilisateur.getAgence() != null ? utilisateur.getAgence().getNom() : null,
                utilisateur.getDateCreation()
        );
    }
}
