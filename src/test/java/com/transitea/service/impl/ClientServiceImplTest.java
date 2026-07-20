package com.transitea.service.impl;

import com.transitea.dto.response.ClientReponse;
import com.transitea.entity.Agence;
import com.transitea.entity.Utilisateur;
import com.transitea.entity.enums.Role;
import com.transitea.exception.AccesNonAutoriseException;
import com.transitea.repository.ColisRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientServiceImplTest {

    @Mock
    private ColisRepository colisRepository;

    @InjectMocks
    private ClientServiceImpl clientService;

    private Agence agence;
    private Utilisateur admin;
    private Utilisateur agent;

    @BeforeEach
    void initialiser() {
        agence = Agence.builder().nom("Agence Paris").ville("Paris").build();
        agence.setId(1L);

        admin = Utilisateur.builder().role(Role.ADMIN).build();
        agent = Utilisateur.builder().role(Role.AGENT).agence(agence).build();
    }

    @Test
    void doit_agreger_tous_les_clients_pour_un_admin() {
        Pageable pageable = PageRequest.of(0, 20);
        ClientReponse client = new ClientReponse("Jean Dupont", "+33612345678", "Lyon", 3);
        when(colisRepository.agregerClients(pageable))
                .thenReturn(new PageImpl<>(java.util.List.of(client)));

        var resultat = clientService.lister(admin, pageable);

        assertThat(resultat.contenu()).hasSize(1);
        assertThat(resultat.contenu().get(0).nombreColis()).isEqualTo(3);
    }

    @Test
    void doit_agreger_les_clients_de_lagence_pour_un_non_admin() {
        Pageable pageable = PageRequest.of(0, 20);
        ClientReponse client = new ClientReponse("Marie Curie", "+33698765432", "Paris", 1);
        when(colisRepository.agregerClientsParAgence(agence, pageable))
                .thenReturn(new PageImpl<>(java.util.List.of(client)));

        var resultat = clientService.lister(agent, pageable);

        assertThat(resultat.contenu()).hasSize(1);
        assertThat(resultat.contenu().get(0).nom()).isEqualTo("Marie Curie");
    }

    @Test
    void doit_refuser_un_utilisateur_sans_agence() {
        Utilisateur sansAgence = Utilisateur.builder().role(Role.AGENT).build();
        Pageable pageable = PageRequest.of(0, 20);

        assertThatThrownBy(() -> clientService.lister(sansAgence, pageable))
                .isInstanceOf(AccesNonAutoriseException.class);
    }
}
