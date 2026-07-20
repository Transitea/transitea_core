package com.transitea.controller;

import com.transitea.entity.Utilisateur;
import com.transitea.entity.enums.Role;
import com.transitea.security.FiltreAuthentificationJwt;
import com.transitea.service.ExportService;
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

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ExportController.class)
@AutoConfigureMockMvc(addFilters = false)
class ExportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ExportService exportService;

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
    void doit_retourner_un_csv_avec_les_bons_en_tetes() throws Exception {
        String csv = "Code Tracking,Expediteur\nTRA-2026-ABC123,Marc Lemoine\n";
        when(exportService.exporterCsvColis(any(), any(), any()))
                .thenReturn(csv.getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(get("/v1/export/colis")
                        .param("debut", "2026-01-01")
                        .param("fin", "2026-12-31"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("colis_2026-01-01_2026-12-31.csv")))
                .andExpect(content().string(csv));
    }

    @Test
    void doit_retourner_400_quand_dates_manquantes() throws Exception {
        mockMvc.perform(get("/v1/export/colis"))
                .andExpect(status().isBadRequest());
    }
}
