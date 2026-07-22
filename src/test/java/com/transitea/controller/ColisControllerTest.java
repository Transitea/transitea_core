package com.transitea.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transitea.dto.request.CreationColisRequete;
import com.transitea.dto.request.MiseAJourStatutRequete;
import com.transitea.dto.response.ColisReponse;
import com.transitea.dto.response.ReponsePagee;
import com.transitea.dto.response.StatistiquesReponse;
import com.transitea.entity.Utilisateur;
import com.transitea.entity.enums.Role;
import com.transitea.entity.enums.StatutColis;
import com.transitea.exception.AccesNonAutoriseException;
import com.transitea.exception.EntiteNonTrouveeException;
import com.transitea.security.FiltreAuthentificationJwt;
import com.transitea.service.ColisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ColisController.class)
@AutoConfigureMockMvc(addFilters = false)
class ColisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ColisService colisService;

    @MockBean
    private FiltreAuthentificationJwt filtreAuthentificationJwt;

    private ColisReponse colisReponse;

    @BeforeEach
    void initialiser() {
        Utilisateur agent = Utilisateur.builder()
                .nom("Girard").prenom("Louis").email("louis.girard@transitea.fr").role(Role.AGENT)
                .build();
        agent.setId(1L);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        agent, null, List.of(new SimpleGrantedAuthority("ROLE_AGENT"))));

        colisReponse = new ColisReponse(
                10L, "uuid-test", "TRA-2026-ABC123",
                1L, "Agence Paris", 2L, "Agence Lyon",
                "Marc Lemoine", null, null,
                "Julie Blanchard", null, null, null, null,
                "Vetements", new BigDecimal("3.500"),
                StatutColis.ENREGISTRE, null, 0, null, List.of());
    }

    @Test
    void doit_retourner_201_quand_creation_valide() throws Exception {
        CreationColisRequete requete = new CreationColisRequete(
                1L, 2L, "Marc Lemoine", null, null, "Julie Blanchard", null, null,
                null, null, "Vetements", new BigDecimal("3.500"), null);

        when(colisService.creer(any(), any())).thenReturn(colisReponse);

        mockMvc.perform(post("/v1/colis")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(requete)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codeTracking").value("TRA-2026-ABC123"));
    }

    @Test
    void doit_retourner_400_quand_agences_manquantes() throws Exception {
        String jsonInvalide = "{\"expediteurNom\": \"Marc\", \"destinataireNom\": \"Julie\"}";

        mockMvc.perform(post("/v1/colis")
                        .contentType("application/json")
                        .content(jsonInvalide))
                .andExpect(status().isBadRequest());
    }

    @Test
    void doit_lister_les_colis_pagines() throws Exception {
        ReponsePagee<ColisReponse> page = ReponsePagee.depuis(new PageImpl<>(List.of(colisReponse), PageRequest.of(0, 20), 1));
        when(colisService.lister(any(), any(), any())).thenReturn(page);

        mockMvc.perform(get("/v1/colis"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenu[0].codeTracking").value("TRA-2026-ABC123"));
    }

    @Test
    void doit_retourner_le_colis_par_id() throws Exception {
        when(colisService.trouverParId(eq(10L), any())).thenReturn(colisReponse);

        mockMvc.perform(get("/v1/colis/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    void doit_retourner_404_quand_colis_inexistant() throws Exception {
        when(colisService.trouverParId(anyLong(), any()))
                .thenThrow(new EntiteNonTrouveeException("Colis", 99L));

        mockMvc.perform(get("/v1/colis/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void doit_retourner_403_quand_acces_non_autorise() throws Exception {
        when(colisService.trouverParId(anyLong(), any()))
                .thenThrow(new AccesNonAutoriseException());

        mockMvc.perform(get("/v1/colis/10"))
                .andExpect(status().isForbidden());
    }

    @Test
    void doit_mettre_a_jour_le_statut() throws Exception {
        MiseAJourStatutRequete requete = new MiseAJourStatutRequete(StatutColis.EN_TRANSIT, "Paris", null);
        when(colisService.mettreAJourStatut(any(), any(), any())).thenReturn(colisReponse);

        mockMvc.perform(patch("/v1/colis/10/statut")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(requete)))
                .andExpect(status().isOk());
    }

    @Test
    void doit_retirer_un_colis_par_scan_qr() throws Exception {
        when(colisService.retirer(any(), any())).thenReturn(colisReponse);

        mockMvc.perform(post("/v1/colis/retrait").param("codeTracking", "TRA-2026-ABC123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codeTracking").value("TRA-2026-ABC123"));
    }

    @Test
    void doit_supprimer_un_colis() throws Exception {
        mockMvc.perform(delete("/v1/colis/10"))
                .andExpect(status().isNoContent());
    }

    @Test
    void doit_retourner_les_statistiques() throws Exception {
        StatistiquesReponse statistiques = new StatistiquesReponse(4, Map.of(StatutColis.ENREGISTRE, 4L));
        when(colisService.obtenirStatistiques(any())).thenReturn(statistiques);

        mockMvc.perform(get("/v1/colis/statistiques"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(4));
    }

    @Test
    void doit_retourner_le_volume_quotidien() throws Exception {
        when(colisService.obtenirVolumeQuotidien(any(), any(), any())).thenReturn(List.of(
                new com.transitea.dto.response.VolumeJourReponse(java.time.LocalDate.of(2026, 7, 1), 5)));

        mockMvc.perform(get("/v1/colis/volume-quotidien?debut=2026-07-01&fin=2026-07-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].total").value(5));
    }
}
