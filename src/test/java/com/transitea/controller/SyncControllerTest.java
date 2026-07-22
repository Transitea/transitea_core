package com.transitea.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transitea.dto.request.CreationColisRequete;
import com.transitea.dto.request.SyncUploadRequete;
import com.transitea.dto.response.SyncDownloadReponse;
import com.transitea.dto.response.SyncUploadReponse;
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
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
    void doit_synchroniser_un_batch_de_colis_en_upload() throws Exception {
        CreationColisRequete colisRequete = new CreationColisRequete(
                1L, 2L, "Marc Lemoine", null, null, "Julie Blanchard", null, null,
                null, null, "Vetements", new BigDecimal("3.500"), 1001L);
        SyncUploadRequete requete = new SyncUploadRequete(List.of(colisRequete), List.of());

        SyncUploadReponse reponse = new SyncUploadReponse(
                1, 1, 0, 0, List.of(),
                0, 0, 0, 0, List.of());
        when(syncService.synchroniserUpload(any(), any())).thenReturn(reponse);

        mockMvc.perform(post("/v1/sync/upload")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(requete)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nbColisReussis").value(1));
    }

    @Test
    void doit_accepter_un_batch_vide_et_renvoyer_des_compteurs_a_zero() throws Exception {
        SyncUploadRequete requete = new SyncUploadRequete(List.of(), List.of());

        SyncUploadReponse reponse = new SyncUploadReponse(
                0, 0, 0, 0, List.of(),
                0, 0, 0, 0, List.of());
        when(syncService.synchroniserUpload(any(), any())).thenReturn(reponse);

        mockMvc.perform(post("/v1/sync/upload")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(requete)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nbColisEnvoyes").value(0));
    }

    @Test
    void doit_rejeter_un_batch_de_plus_de_100_colis() throws Exception {
        CreationColisRequete colisRequete = new CreationColisRequete(
                1L, 2L, "Marc Lemoine", null, null, "Julie Blanchard", null, null,
                null, null, "Vetements", new BigDecimal("3.500"), 1001L);
        SyncUploadRequete requete = new SyncUploadRequete(
                java.util.Collections.nCopies(101, colisRequete), List.of());

        mockMvc.perform(post("/v1/sync/upload")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(requete)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void doit_telecharger_les_colis_modifies_depuis_une_date() throws Exception {
        LocalDateTime depuis = LocalDateTime.now().minusHours(1);
        SyncDownloadReponse reponse = new SyncDownloadReponse(List.of(), LocalDateTime.now());
        when(syncService.synchroniserDownload(eq(depuis), any())).thenReturn(reponse);

        mockMvc.perform(get("/v1/sync/download")
                        .param("since", depuis.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.colis").isArray());
    }
}
