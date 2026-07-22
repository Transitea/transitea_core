package com.transitea.controller;

import com.transitea.dto.response.ClientReponse;
import com.transitea.dto.response.ReponsePagee;
import com.transitea.entity.Utilisateur;
import com.transitea.entity.enums.Role;
import com.transitea.security.FiltreAuthentificationJwt;
import com.transitea.service.ClientService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ClientController.class)
@AutoConfigureMockMvc(addFilters = false)
class ClientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ClientService clientService;

    @MockBean
    private FiltreAuthentificationJwt filtreAuthentificationJwt;

    private Utilisateur authentifier(Role role) {
        Utilisateur utilisateur = Utilisateur.builder()
                .nom("Test").prenom("Test").email("test@transitea.fr").role(role)
                .build();
        utilisateur.setId(1L);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        utilisateur, null, List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))));
        return utilisateur;
    }

    @Test
    void doit_lister_les_clients_agreges() throws Exception {
        authentifier(Role.ADMIN);
        ClientReponse client = new ClientReponse("Jean Dupont", "+33612345678", "Lyon", 3);
        when(clientService.lister(any(), any()))
                .thenReturn(new ReponsePagee<>(List.of(client), 0, 1, 1, 20, true));

        mockMvc.perform(get("/v1/clients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenu[0].nom").value("Jean Dupont"))
                .andExpect(jsonPath("$.contenu[0].nombreColis").value(3));
    }
}
