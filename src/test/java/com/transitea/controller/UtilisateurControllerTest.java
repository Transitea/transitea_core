package com.transitea.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transitea.dto.request.CreationUtilisateurRequete;
import com.transitea.dto.request.MiseAJourStatutUtilisateurRequete;
import com.transitea.dto.response.ReponsePagee;
import com.transitea.dto.response.UtilisateurReponse;
import com.transitea.entity.Utilisateur;
import com.transitea.entity.enums.Role;
import com.transitea.entity.enums.StatutUtilisateur;
import com.transitea.security.FiltreAuthentificationJwt;
import com.transitea.service.UtilisateurService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UtilisateurController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(UtilisateurControllerTest.ConfigurationMethodSecurityTest.class)
class UtilisateurControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class ConfigurationMethodSecurityTest {
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UtilisateurService utilisateurService;

    @MockBean
    private FiltreAuthentificationJwt filtreAuthentificationJwt;

    private void authentifier(Role role) {
        Utilisateur utilisateur = Utilisateur.builder()
                .nom("Test").prenom("Test").email("test@transitea.fr").role(role)
                .build();
        utilisateur.setId(1L);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        utilisateur, null, List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))));
    }

    private UtilisateurReponse utilisateurReponse() {
        return new UtilisateurReponse(
                2L, "uuid-test", "Renard", "Sophie", "sophie.renard@transitea.fr",
                "+33676543210", Role.AGENT, StatutUtilisateur.ACTIF, 1L, "Agence Paris",
                LocalDateTime.now());
    }

    @Test
    void doit_autoriser_admin_a_lister_les_utilisateurs() throws Exception {
        authentifier(Role.ADMIN);
        ReponsePagee<UtilisateurReponse> page = ReponsePagee.depuis(
                new PageImpl<>(List.of(utilisateurReponse()), PageRequest.of(0, 20), 1));
        when(utilisateurService.lister(any(), any())).thenReturn(page);

        mockMvc.perform(get("/v1/utilisateurs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenu[0].email").value("sophie.renard@transitea.fr"));
    }

    @Test
    void doit_refuser_la_liste_a_un_agent() throws Exception {
        authentifier(Role.AGENT);

        mockMvc.perform(get("/v1/utilisateurs"))
                .andExpect(status().isForbidden());
    }

    @Test
    void doit_refuser_la_liste_a_un_operateur() throws Exception {
        authentifier(Role.OPERATEUR);

        mockMvc.perform(get("/v1/utilisateurs"))
                .andExpect(status().isForbidden());
    }

    @Test
    void doit_autoriser_admin_a_creer_un_compte() throws Exception {
        authentifier(Role.ADMIN);
        CreationUtilisateurRequete requete = new CreationUtilisateurRequete(
                "Renard", "Sophie", "sophie.renard@transitea.fr",
                "+33676543210", "motdepasse123", Role.AGENT, 1L);
        when(utilisateurService.creer(any())).thenReturn(utilisateurReponse());

        mockMvc.perform(post("/v1/utilisateurs")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(requete)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("AGENT"));
    }

    @Test
    void doit_refuser_la_creation_a_un_agent() throws Exception {
        authentifier(Role.AGENT);
        CreationUtilisateurRequete requete = new CreationUtilisateurRequete(
                "Renard", "Sophie", "sophie.renard@transitea.fr",
                "+33676543210", "motdepasse123", Role.AGENT, 1L);

        mockMvc.perform(post("/v1/utilisateurs")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(requete)))
                .andExpect(status().isForbidden());
    }

    @Test
    void doit_autoriser_admin_a_desactiver_un_compte() throws Exception {
        authentifier(Role.ADMIN);
        MiseAJourStatutUtilisateurRequete requete = new MiseAJourStatutUtilisateurRequete(StatutUtilisateur.INACTIF);
        when(utilisateurService.mettreAJourStatut(any(), any())).thenReturn(utilisateurReponse());

        mockMvc.perform(patch("/v1/utilisateurs/2/statut")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(requete)))
                .andExpect(status().isOk());
    }

    @Test
    void doit_refuser_la_desactivation_a_un_operateur() throws Exception {
        authentifier(Role.OPERATEUR);
        MiseAJourStatutUtilisateurRequete requete = new MiseAJourStatutUtilisateurRequete(StatutUtilisateur.INACTIF);

        mockMvc.perform(patch("/v1/utilisateurs/2/statut")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(requete)))
                .andExpect(status().isForbidden());
    }
}
