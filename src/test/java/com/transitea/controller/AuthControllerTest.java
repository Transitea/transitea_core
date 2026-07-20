package com.transitea.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transitea.dto.request.ConnexionRequete;
import com.transitea.dto.request.InscriptionRequete;
import com.transitea.dto.request.RafraichissementRequete;
import com.transitea.dto.response.AuthReponse;
import com.transitea.dto.response.UtilisateurReponse;
import com.transitea.entity.Utilisateur;
import com.transitea.entity.enums.Role;
import com.transitea.entity.enums.StatutUtilisateur;
import com.transitea.exception.ErreurMetier;
import com.transitea.security.FiltreAuthentificationJwt;
import com.transitea.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private FiltreAuthentificationJwt filtreAuthentificationJwt;

    private Utilisateur utilisateur;

    @BeforeEach
    void initialiser() {
        utilisateur = Utilisateur.builder()
                .nom("Girard")
                .prenom("Louis")
                .email("louis.girard@transitea.fr")
                .role(Role.AGENT)
                .build();
        utilisateur.setId(1L);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        utilisateur, null, List.of(new SimpleGrantedAuthority("ROLE_AGENT"))));
    }

    private UtilisateurReponse utilisateurReponse() {
        return new UtilisateurReponse(
                1L, "uuid-test", "Girard", "Louis", "louis.girard@transitea.fr",
                "+33611223344", Role.AGENT, StatutUtilisateur.ACTIF, 1L, "Agence Paris",
                LocalDateTime.now());
    }

    @Test
    void doit_retourner_201_quand_inscription_valide() throws Exception {
        InscriptionRequete requete = new InscriptionRequete(
                1L, "Girard", "Louis", "louis.girard@transitea.fr",
                "+33611223344", "motdepasse123");

        AuthReponse reponse = AuthReponse.creer("access", "refresh", 3600000L, utilisateurReponse());
        when(authService.inscrire(any())).thenReturn(reponse);

        mockMvc.perform(post("/v1/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(requete)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("access"))
                .andExpect(jsonPath("$.utilisateur.email").value("louis.girard@transitea.fr"));
    }

    @Test
    void doit_retourner_400_quand_champs_obligatoires_manquants() throws Exception {
        String jsonInvalide = "{\"nom\": \"\", \"email\": \"pas-un-email\"}";

        mockMvc.perform(post("/v1/auth/register")
                        .contentType("application/json")
                        .content(jsonInvalide))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erreursValidation").exists());
    }

    @Test
    void doit_retourner_200_quand_connexion_valide() throws Exception {
        ConnexionRequete requete = new ConnexionRequete("louis.girard@transitea.fr", "motdepasse123");
        AuthReponse reponse = AuthReponse.creer("access", "refresh", 3600000L, utilisateurReponse());
        when(authService.connecter(any())).thenReturn(reponse);

        mockMvc.perform(post("/v1/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(requete)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access"));
    }

    @Test
    void doit_retourner_400_quand_erreur_metier_sur_connexion() throws Exception {
        ConnexionRequete requete = new ConnexionRequete("louis.girard@transitea.fr", "mauvais");
        when(authService.connecter(any())).thenThrow(new ErreurMetier("Email ou mot de passe incorrect"));

        mockMvc.perform(post("/v1/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(requete)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Email ou mot de passe incorrect"));
    }

    @Test
    void doit_retourner_200_quand_rafraichissement_valide() throws Exception {
        RafraichissementRequete requete = new RafraichissementRequete("refresh-token-valide");
        AuthReponse reponse = AuthReponse.creer("nouveau-access", "nouveau-refresh", 3600000L, utilisateurReponse());
        when(authService.rafraichir(any())).thenReturn(reponse);

        mockMvc.perform(post("/v1/auth/refresh")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(requete)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("nouveau-access"));
    }

    @Test
    void doit_retourner_204_quand_deconnexion() throws Exception {
        RafraichissementRequete requete = new RafraichissementRequete("refresh-token-valide");

        mockMvc.perform(post("/v1/auth/logout")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(requete)))
                .andExpect(status().isNoContent());
    }

    @Test
    void doit_retourner_le_profil_de_lutilisateur_connecte() throws Exception {
        when(authService.obtenirProfil(any())).thenReturn(utilisateurReponse());

        mockMvc.perform(get("/v1/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("louis.girard@transitea.fr"));
    }
}
