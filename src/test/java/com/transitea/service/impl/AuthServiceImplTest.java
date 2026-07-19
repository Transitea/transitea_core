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
import com.transitea.exception.TokenInvalideException;
import com.transitea.repository.AgenceRepository;
import com.transitea.repository.RefreshTokenRepository;
import com.transitea.repository.UtilisateurRepository;
import com.transitea.security.ProprietesJwt;
import com.transitea.security.ServiceJwt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UtilisateurRepository utilisateurRepository;

    @Mock
    private AgenceRepository agenceRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private ServiceJwt serviceJwt;

    @Mock
    private PasswordEncoder encodeurMotDePasse;

    @Mock
    private AuthenticationManager gestionnaireAuthentification;

    @Mock
    private ProprietesJwt proprietesJwt;

    @InjectMocks
    private AuthServiceImpl authService;

    private Utilisateur utilisateur;
    private Agence agence;
    private RefreshToken refreshTokenValide;

    @BeforeEach
    void initialiser() {
        agence = Agence.builder().nom("Agence Kinshasa").ville("Kinshasa").build();
        agence.setId(1L);

        utilisateur = Utilisateur.builder()
                .nom("Lumbu")
                .prenom("Louange")
                .email("louange@transitea.cd")
                .telephone("0812345678")
                .motDePasseHash("hash_mot_de_passe")
                .role(Role.AGENT)
                .agence(agence)
                .build();
        utilisateur.setId(1L);

        refreshTokenValide = RefreshToken.builder()
                .utilisateur(utilisateur)
                .token("refresh-token-valide")
                .dateExpiration(LocalDateTime.now().plusDays(7))
                .revoque(false)
                .build();
    }

    // --- inscrire ---

    @Test
    void doit_creer_utilisateur_et_retourner_tokens_quand_email_disponible() {
        InscriptionRequete requete = new InscriptionRequete(
                1L, "Lumbu", "Louange", "louange@transitea.cd",
                "0812345678", "MotDePasse123!");
        when(utilisateurRepository.existsByEmail(requete.email())).thenReturn(false);
        when(utilisateurRepository.existsByTelephone(requete.telephone())).thenReturn(false);
        when(agenceRepository.findByIdAndSupprimeFalse(1L)).thenReturn(Optional.of(agence));
        when(encodeurMotDePasse.encode(anyString())).thenReturn("hash_mot_de_passe");
        when(utilisateurRepository.save(any(Utilisateur.class))).thenReturn(utilisateur);
        when(serviceJwt.genererAccessToken(any())).thenReturn("access-token");
        when(serviceJwt.genererRefreshToken(any())).thenReturn("refresh-token");
        when(proprietesJwt.expirationAccessMs()).thenReturn(3600000L);
        when(refreshTokenRepository.save(any())).thenReturn(refreshTokenValide);

        AuthReponse resultat = authService.inscrire(requete);

        assertThat(resultat).isNotNull();
        assertThat(resultat.accessToken()).isEqualTo("access-token");
        assertThat(resultat.utilisateur().email()).isEqualTo("louange@transitea.cd");

        ArgumentCaptor<Utilisateur> captor = ArgumentCaptor.forClass(Utilisateur.class);
        verify(utilisateurRepository).save(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo(Role.AGENT);
    }

    @Test
    void doit_lancer_erreur_metier_quand_email_deja_utilise() {
        InscriptionRequete requete = new InscriptionRequete(
                1L, "Lumbu", "Louange", "louange@transitea.cd",
                null, "MotDePasse123!");
        when(utilisateurRepository.existsByEmail(requete.email())).thenReturn(true);

        assertThatThrownBy(() -> authService.inscrire(requete))
                .isInstanceOf(ErreurMetier.class)
                .hasMessageContaining("email");

        verify(utilisateurRepository, never()).save(any());
    }

    @Test
    void doit_lancer_erreur_metier_quand_telephone_deja_utilise() {
        InscriptionRequete requete = new InscriptionRequete(
                1L, "Lumbu", "Louange", "nouveau@transitea.cd",
                "0812345678", "MotDePasse123!");
        when(utilisateurRepository.existsByEmail(requete.email())).thenReturn(false);
        when(utilisateurRepository.existsByTelephone(requete.telephone())).thenReturn(true);

        assertThatThrownBy(() -> authService.inscrire(requete))
                .isInstanceOf(ErreurMetier.class)
                .hasMessageContaining("telephone");

        verify(utilisateurRepository, never()).save(any());
    }

    // --- connecter ---

    @Test
    void doit_retourner_tokens_quand_credentiels_valides() {
        ConnexionRequete requete = new ConnexionRequete("louange@transitea.cd", "MotDePasse123!");
        when(utilisateurRepository.findByEmailAndSupprimeFalse(requete.email()))
                .thenReturn(Optional.of(utilisateur));
        when(serviceJwt.genererAccessToken(any())).thenReturn("access-token");
        when(serviceJwt.genererRefreshToken(any())).thenReturn("refresh-token");
        when(proprietesJwt.expirationAccessMs()).thenReturn(3600000L);
        when(refreshTokenRepository.save(any())).thenReturn(refreshTokenValide);

        AuthReponse resultat = authService.connecter(requete);

        assertThat(resultat.accessToken()).isEqualTo("access-token");
        verify(refreshTokenRepository).revoquerTousLesTokensUtilisateur(utilisateur);
    }

    @Test
    void doit_lancer_erreur_metier_quand_mauvais_credentiels() {
        ConnexionRequete requete = new ConnexionRequete("louange@transitea.cd", "MauvaisMotDePasse");
        when(gestionnaireAuthentification.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.connecter(requete))
                .isInstanceOf(ErreurMetier.class)
                .hasMessageContaining("incorrect");

        verify(refreshTokenRepository, never()).save(any());
    }

    // --- rafraichir ---

    @Test
    void doit_retourner_nouveaux_tokens_quand_refresh_token_valide() {
        RafraichissementRequete requete = new RafraichissementRequete("refresh-token-valide");
        when(serviceJwt.validerRefreshToken("refresh-token-valide")).thenReturn(true);
        when(refreshTokenRepository.findByToken("refresh-token-valide"))
                .thenReturn(Optional.of(refreshTokenValide));
        when(serviceJwt.genererAccessToken(any())).thenReturn("nouveau-access-token");
        when(serviceJwt.genererRefreshToken(any())).thenReturn("nouveau-refresh-token");
        when(proprietesJwt.expirationAccessMs()).thenReturn(3600000L);
        when(refreshTokenRepository.save(any())).thenReturn(refreshTokenValide);

        AuthReponse resultat = authService.rafraichir(requete);

        assertThat(resultat.accessToken()).isEqualTo("nouveau-access-token");
        assertThat(refreshTokenValide.getRevoque()).isTrue();
    }

    @Test
    void doit_lancer_token_invalide_exception_quand_jwt_invalide() {
        RafraichissementRequete requete = new RafraichissementRequete("token-expire");
        when(serviceJwt.validerRefreshToken("token-expire")).thenReturn(false);

        assertThatThrownBy(() -> authService.rafraichir(requete))
                .isInstanceOf(TokenInvalideException.class);

        verify(refreshTokenRepository, never()).findByToken(anyString());
    }

    @Test
    void doit_lancer_token_invalide_exception_quand_token_revoque() {
        RafraichissementRequete requete = new RafraichissementRequete("token-revoque");
        RefreshToken tokenRevoque = RefreshToken.builder()
                .utilisateur(utilisateur)
                .token("token-revoque")
                .dateExpiration(LocalDateTime.now().plusDays(7))
                .revoque(true)
                .build();
        when(serviceJwt.validerRefreshToken("token-revoque")).thenReturn(true);
        when(refreshTokenRepository.findByToken("token-revoque"))
                .thenReturn(Optional.of(tokenRevoque));

        assertThatThrownBy(() -> authService.rafraichir(requete))
                .isInstanceOf(TokenInvalideException.class);

        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    @Test
    void doit_lancer_token_invalide_exception_quand_token_expire() {
        RafraichissementRequete requete = new RafraichissementRequete("token-expire-bdd");
        RefreshToken tokenExpire = RefreshToken.builder()
                .utilisateur(utilisateur)
                .token("token-expire-bdd")
                .dateExpiration(LocalDateTime.now().minusHours(1))
                .revoque(false)
                .build();
        when(serviceJwt.validerRefreshToken("token-expire-bdd")).thenReturn(true);
        when(refreshTokenRepository.findByToken("token-expire-bdd"))
                .thenReturn(Optional.of(tokenExpire));

        assertThatThrownBy(() -> authService.rafraichir(requete))
                .isInstanceOf(TokenInvalideException.class);
    }

    // --- deconnecter ---

    @Test
    void doit_revoquer_refresh_token_quand_token_existe() {
        when(refreshTokenRepository.findByToken("refresh-token-valide"))
                .thenReturn(Optional.of(refreshTokenValide));
        when(refreshTokenRepository.save(any())).thenReturn(refreshTokenValide);

        authService.deconnecter("refresh-token-valide");

        assertThat(refreshTokenValide.getRevoque()).isTrue();
        verify(refreshTokenRepository).save(refreshTokenValide);
    }

    @Test
    void doit_ne_pas_lancer_exception_quand_token_inexistant() {
        when(refreshTokenRepository.findByToken("token-inconnu"))
                .thenReturn(Optional.empty());

        authService.deconnecter("token-inconnu");

        verify(refreshTokenRepository, never()).save(any());
    }

    // --- obtenirProfil ---

    @Test
    void doit_retourner_les_infos_utilisateur() {
        UtilisateurReponse reponse = authService.obtenirProfil(utilisateur);

        assertThat(reponse.email()).isEqualTo("louange@transitea.cd");
        assertThat(reponse.nom()).isEqualTo("Lumbu");
        assertThat(reponse.role()).isEqualTo(Role.AGENT);
    }
}
