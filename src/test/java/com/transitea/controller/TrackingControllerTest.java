package com.transitea.controller;

import com.transitea.dto.response.SuiviPublicReponse;
import com.transitea.entity.enums.StatutColis;
import com.transitea.exception.EntiteNonTrouveeException;
import com.transitea.security.FiltreAuthentificationJwt;
import com.transitea.service.TrackingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TrackingController.class)
@AutoConfigureMockMvc(addFilters = false)
class TrackingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TrackingService trackingService;

    @MockBean
    private FiltreAuthentificationJwt filtreAuthentificationJwt;

    @Test
    void doit_retourner_le_suivi_public_dun_colis() throws Exception {
        SuiviPublicReponse reponse = new SuiviPublicReponse(
                "TRA-2026-ABC123", "Marc Lemoine", "Julie Blanchard", "Lyon",
                "Vetements", new BigDecimal("3.500"), StatutColis.EN_TRANSIT, null, List.of());
        when(trackingService.suivreParCode("TRA-2026-ABC123")).thenReturn(reponse);

        mockMvc.perform(get("/v1/tracking/TRA-2026-ABC123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codeTracking").value("TRA-2026-ABC123"))
                .andExpect(jsonPath("$.statutActuel").value("EN_TRANSIT"));
    }

    @Test
    void doit_retourner_404_quand_code_tracking_inconnu() throws Exception {
        when(trackingService.suivreParCode(anyString()))
                .thenThrow(new EntiteNonTrouveeException("Colis", "TRA-INCONNU"));

        mockMvc.perform(get("/v1/tracking/TRA-INCONNU"))
                .andExpect(status().isNotFound());
    }
}
