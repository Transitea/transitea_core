package com.transitea.service.impl;

import com.transitea.dto.request.ConnexionRequete;
import com.transitea.dto.request.InscriptionRequete;
import com.transitea.dto.request.RafraichissementRequete;
import com.transitea.dto.response.AuthReponse;
import com.transitea.dto.response.UtilisateurReponse;
import com.transitea.entity.Agence;
import com.transitea.entity.RefreshToken;
import com.transitea.entity.Utilisateur;
import com.transitea.entity.enums.Role;
import com.transitea.exception.ErreurMetier;
import com.transitea.exception.EntiteNonTrouveeException;
import com.transitea.exception.TokenInvalideException;
import com.transitea.repository.AgenceRepository;
import com.transitea.repository.RefreshTokenRepository;
import com.transitea.repository.UtilisateurRepository;
import com.transitea.security.ProprietesJwt;
import com.transitea.security.ServiceJwt;
import com.transitea.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    private static final Logger journal = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UtilisateurRepository utilisateurRepository;
    private final AgenceRepository agenceRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final ServiceJwt serviceJwt;
    private final PasswordEncoder encodeurMotDePasse;
    private final AuthenticationManager gestionnaireAuthentification;
    private final ProprietesJwt proprietesJwt;

    public AuthServiceImpl(
            UtilisateurRepository utilisateurRepository,
            AgenceRepository agenceRepository,
            RefreshTokenRepository refreshTokenRepository,
            ServiceJwt serviceJwt,
            PasswordEncoder encodeurMotDePasse,
            AuthenticationManager gestionnaireAuthentification,
            ProprietesJwt proprietesJwt) {
        this.utilisateurRepository = utilisateurRepository;
        this.agenceRepository = agenceRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.serviceJwt = serviceJwt;
        this.encodeurMotDePasse = encodeurMotDePasse;
        this.gestionnaireAuthentification = gestionnaireAuthentification;
        this.proprietesJwt = proprietesJwt;
    }

    @Override
    public AuthReponse inscrire(InscriptionRequete requete) {
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
                .role(Role.AGENT)
                .agence(agence)
                .build();

        Utilisateur sauvegarde = utilisateurRepository.save(utilisateur);
        journal.info("Nouvel utilisateur inscrit : {}", sauvegarde.getEmail());

        return genererReponseAuth(sauvegarde);
    }

    @Override
    public AuthReponse connecter(ConnexionRequete requete) {
        try {
            gestionnaireAuthentification.authenticate(
                    new UsernamePasswordAuthenticationToken(requete.email(), requete.motDePasse()));
        } catch (BadCredentialsException e) {
            throw new ErreurMetier("Email ou mot de passe incorrect");
        }

        Utilisateur utilisateur = utilisateurRepository.findByEmailAndSupprimeFalse(requete.email())
                .orElseThrow(() -> new EntiteNonTrouveeException("Utilisateur", requete.email()));

        refreshTokenRepository.revoquerTousLesTokensUtilisateur(utilisateur);

        journal.info("Connexion reussie pour : {}", utilisateur.getEmail());
        return genererReponseAuth(utilisateur);
    }

    @Override
    public AuthReponse rafraichir(RafraichissementRequete requete) {
        String tokenRecu = requete.refreshToken();

        if (!serviceJwt.validerRefreshToken(tokenRecu)) {
            throw new TokenInvalideException("Refresh token invalide ou expire");
        }

        RefreshToken refreshToken = refreshTokenRepository.findByToken(tokenRecu)
                .orElseThrow(TokenInvalideException::new);

        if (!refreshToken.estValide()) {
            throw new TokenInvalideException("Refresh token revoque ou expire");
        }

        refreshToken.setRevoque(true);
        refreshTokenRepository.save(refreshToken);

        Utilisateur utilisateur = refreshToken.getUtilisateur();
        journal.info("Tokens rafraichis pour : {}", utilisateur.getEmail());

        return genererReponseAuth(utilisateur);
    }

    @Override
    public void deconnecter(String refreshToken) {
        refreshTokenRepository.findByToken(refreshToken).ifPresent(token -> {
            token.setRevoque(true);
            refreshTokenRepository.save(token);
            journal.info("Deconnexion : token revoque pour {}", token.getUtilisateur().getEmail());
        });
    }

    @Override
    @Transactional(readOnly = true)
    public UtilisateurReponse obtenirProfil(Utilisateur utilisateur) {
        return versUtilisateurReponse(utilisateur);
    }

    private AuthReponse genererReponseAuth(Utilisateur utilisateur) {
        String accessToken = serviceJwt.genererAccessToken(utilisateur);
        String refreshToken = serviceJwt.genererRefreshToken(utilisateur);

        RefreshToken entiteRefreshToken = RefreshToken.builder()
                .utilisateur(utilisateur)
                .token(refreshToken)
                .dateExpiration(LocalDateTime.now().plusNanos(
                        proprietesJwt.expirationRefreshMs() * 1_000_000))
                .build();

        refreshTokenRepository.save(entiteRefreshToken);

        return AuthReponse.creer(
                accessToken,
                refreshToken,
                proprietesJwt.expirationAccessMs(),
                versUtilisateurReponse(utilisateur)
        );
    }

    private UtilisateurReponse versUtilisateurReponse(Utilisateur utilisateur) {
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
