package com.transitea.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transitea.dto.request.CreationAgenceRequete;
import com.transitea.dto.response.AgenceReponse;
import com.transitea.entity.Utilisateur;
import com.transitea.entity.enums.Role;
import com.transitea.security.FiltreAuthentificationJwt;
import com.transitea.service.AgenceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AgenceController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(AgenceControllerTest.ConfigurationMethodSecurityTest.class)
class AgenceControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class ConfigurationMethodSecurityTest {
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AgenceService agenceService;

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

    private AgenceReponse agenceReponse() {
        return new AgenceReponse(1L, "uuid-test", "Agence Paris", "Paris",
                "12 Rue de Rivoli, 75004 Paris", 1L, LocalDateTime.now());
    }

    @Test
    void doit_autoriser_admin_a_creer_une_agence() throws Exception {
        authentifier(Role.ADMIN);
        CreationAgenceRequete requete = new CreationAgenceRequete("Agence Nice", "Nice", "1 Promenade des Anglais");
        when(agenceService.creer(any())).thenReturn(agenceReponse());

        mockMvc.perform(post("/v1/agences")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(requete)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nom").value("Agence Paris"));
    }

    @Test
    void doit_refuser_la_creation_a_un_agent() throws Exception {
        authentifier(Role.AGENT);
        CreationAgenceRequete requete = new CreationAgenceRequete("Agence Nice", "Nice", "1 Promenade des Anglais");

        mockMvc.perform(post("/v1/agences")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(requete)))
                .andExpect(status().isForbidden());
    }

    @Test
    void doit_refuser_la_creation_a_un_operateur() throws Exception {
        authentifier(Role.OPERATEUR);
        CreationAgenceRequete requete = new CreationAgenceRequete("Agence Nice", "Nice", "1 Promenade des Anglais");

        mockMvc.perform(post("/v1/agences")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(requete)))
                .andExpect(status().isForbidden());
    }

    @Test
    void doit_lister_les_agences() throws Exception {
        authentifier(Role.AGENT);
        when(agenceService.lister()).thenReturn(List.of(agenceReponse()));

        mockMvc.perform(get("/v1/agences"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nom").value("Agence Paris"));
    }

    @Test
    void doit_retourner_le_detail_dune_agence() throws Exception {
        authentifier(Role.AGENT);
        when(agenceService.trouverParId(1L)).thenReturn(agenceReponse());

        mockMvc.perform(get("/v1/agences/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ville").value("Paris"));
    }
}
