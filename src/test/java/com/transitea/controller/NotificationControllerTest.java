package com.transitea.controller;

import com.transitea.dto.response.NotificationReponse;
import com.transitea.dto.response.ReponsePagee;
import com.transitea.entity.Utilisateur;
import com.transitea.entity.enums.Role;
import com.transitea.entity.enums.StatutNotification;
import com.transitea.entity.enums.TypeCanal;
import com.transitea.security.FiltreAuthentificationJwt;
import com.transitea.service.NotificationService;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = NotificationController.class)
@AutoConfigureMockMvc(addFilters = false)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationService notificationService;

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
    void doit_lister_les_notifications() throws Exception {
        authentifier(Role.ADMIN);
        NotificationReponse notif = new NotificationReponse(
                1L, 5L, "TRA-2026-000001", "marie@example.com", "DESTINATAIRE",
                TypeCanal.EMAIL, "Colis enregistre", StatutNotification.ENVOYE, 1, LocalDateTime.now());
        when(notificationService.lister(any(), any()))
                .thenReturn(new ReponsePagee<>(List.of(notif), 0, 1, 1, 20, true));

        mockMvc.perform(get("/v1/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenu[0].codeTracking").value("TRA-2026-000001"))
                .andExpect(jsonPath("$.contenu[0].cible").value("DESTINATAIRE"));
    }
}
