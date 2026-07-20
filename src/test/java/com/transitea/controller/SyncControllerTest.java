package com.transitea.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transitea.dto.request.CreationColisRequete;
import com.transitea.dto.request.SyncRequete;
import com.transitea.dto.response.SyncReponse;
import com.transitea.entity.Utilisateur;
import com.transitea.entity.enums.Role;
import com.transitea.security.FiltreAuthentificationJwt;
import com.transitea.service.SyncService;
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

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SyncController.class)
@AutoConfigureMockMvc(addFilters = false)
class SyncControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SyncService syncService;

    @MockBean
    private FiltreAuthentificationJwt filtreAuthentificationJwt;

    @BeforeEach
    void initialiser() {
        Utilisateur agent = Utilisateur.builder()
                .nom("Girard").prenom("Louis").email("louis.girard@transitea.fr").role(Role.AGENT)
                .build();
        agent.setId(1L);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        agent, null, List.of(new SimpleGrantedAuthority("ROLE_AGENT"))));
    }

    @Test
    void doit_synchroniser_un_batch_de_colis() throws Exception {
        CreationColisRequete colisRequete = new CreationColisRequete(
                1L, 2L, "Marc Lemoine", null, null, "Julie Blanchard", null, null,
                null, null, "Vetements", new BigDecimal("3.500"), 1001L);
        SyncRequete requete = new SyncRequete(List.of(colisRequete));

        SyncReponse reponse = new SyncReponse(1, 1, 0, 0, List.of());
        when(syncService.synchroniser(any(), any())).thenReturn(reponse);

        mockMvc.perform(post("/v1/sync")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(requete)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nbReussis").value(1));
    }

    @Test
    void doit_retourner_400_quand_liste_colis_vide() throws Exception {
        SyncRequete requete = new SyncRequete(List.of());

        mockMvc.perform(post("/v1/sync")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(requete)))
                .andExpect(status().isBadRequest());
    }
}
