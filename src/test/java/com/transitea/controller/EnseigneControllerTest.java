package com.transitea.controller;

import com.transitea.dto.response.EnseigneReponse;
import com.transitea.entity.Utilisateur;
import com.transitea.entity.enums.PalierAbonnement;
import com.transitea.entity.enums.Role;
import com.transitea.entity.enums.StatutEnseigne;
import com.transitea.security.FiltreAuthentificationJwt;
import com.transitea.service.EnseigneService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = EnseigneController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(EnseigneControllerTest.ConfigurationMethodSecurityTest.class)
class EnseigneControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class ConfigurationMethodSecurityTest {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EnseigneService enseigneService;

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

    @Test
    void doit_autoriser_admin_a_consulter_le_quota() throws Exception {
        authentifier(Role.ADMIN);
        when(enseigneService.obtenir()).thenReturn(new EnseigneReponse(
                1L, "Transitea France", PalierAbonnement.STANDARD, 100, 42, 42.0, StatutEnseigne.ACTIF));

        mockMvc.perform(get("/v1/enseigne"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nom").value("Transitea France"))
                .andExpect(jsonPath("$.pourcentageConsomme").value(42.0));
    }

    @Test
    void doit_refuser_laccess_a_un_agent() throws Exception {
        authentifier(Role.AGENT);

        mockMvc.perform(get("/v1/enseigne"))
                .andExpect(status().isForbidden());
    }

    @Test
    void doit_refuser_laccess_a_un_operateur() throws Exception {
        authentifier(Role.OPERATEUR);

        mockMvc.perform(get("/v1/enseigne"))
                .andExpect(status().isForbidden());
    }
}
