package com.transitea.service.impl;

import com.transitea.dto.request.CreationUtilisateurRequete;
import com.transitea.dto.response.ReponsePagee;
import com.transitea.dto.response.UtilisateurReponse;
import com.transitea.entity.Agence;
import com.transitea.entity.Utilisateur;
import com.transitea.entity.enums.Role;
import com.transitea.entity.enums.StatutUtilisateur;
import com.transitea.exception.EntiteNonTrouveeException;
import com.transitea.exception.ErreurMetier;
import com.transitea.repository.AgenceRepository;
import com.transitea.repository.UtilisateurRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UtilisateurServiceImplTest {

    @Mock
    private UtilisateurRepository utilisateurRepository;

    @Mock
    private AgenceRepository agenceRepository;

    @Mock
    private PasswordEncoder encodeurMotDePasse;

    @InjectMocks
    private UtilisateurServiceImpl utilisateurService;

    private Agence agence;
    private Utilisateur agent;

    @BeforeEach
    void initialiser() {
        agence = Agence.builder().nom("Agence Paris").ville("Paris").build();
        agence.setId(1L);

        agent = Utilisateur.builder()
                .nom("Girard")
                .prenom("Louis")
                .email("louis.girard@transitea.fr")
                .telephone("+33611223344")
                .motDePasseHash("hash")
                .role(Role.AGENT)
                .agence(agence)
                .build();
        agent.setId(10L);
    }

    // --- lister ---

    @Test
    void doit_lister_tous_les_utilisateurs_quand_agence_non_precisee() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Utilisateur> page = new PageImpl<>(List.of(agent));
        when(utilisateurRepository.findBySupprimeFalse(pageable)).thenReturn(page);

        ReponsePagee<UtilisateurReponse> resultat = utilisateurService.lister(null, pageable);

        assertThat(resultat.contenu()).hasSize(1);
        verify(utilisateurRepository, never()).findByAgenceAndSupprimeFalse(any(), any());
    }

    @Test
    void doit_filtrer_par_agence_quand_agenceId_precise() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Utilisateur> page = new PageImpl<>(List.of(agent));
        when(agenceRepository.findByIdAndSupprimeFalse(1L)).thenReturn(Optional.of(agence));
        when(utilisateurRepository.findByAgenceAndSupprimeFalse(agence, pageable)).thenReturn(page);

        ReponsePagee<UtilisateurReponse> resultat = utilisateurService.lister(1L, pageable);

        assertThat(resultat.contenu()).hasSize(1);
        assertThat(resultat.contenu().get(0).agenceNom()).isEqualTo("Agence Paris");
    }

    @Test
    void doit_lancer_exception_quand_agence_inexistante() {
        Pageable pageable = PageRequest.of(0, 20);
        when(agenceRepository.findByIdAndSupprimeFalse(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> utilisateurService.lister(99L, pageable))
                .isInstanceOf(EntiteNonTrouveeException.class);
    }

    // --- creer ---

    @Test
    void doit_creer_un_agent_rattache_a_une_agence() {
        CreationUtilisateurRequete requete = new CreationUtilisateurRequete(
                "Renard", "Sophie", "sophie.renard@transitea.fr",
                "+33676543210", "motdepasse123", Role.AGENT, 1L);

        when(utilisateurRepository.existsByEmail(requete.email())).thenReturn(false);
        when(utilisateurRepository.existsByTelephone(requete.telephone())).thenReturn(false);
        when(agenceRepository.findByIdAndSupprimeFalse(1L)).thenReturn(Optional.of(agence));
        when(encodeurMotDePasse.encode(anyString())).thenReturn("hash_encode");
        when(utilisateurRepository.save(any(Utilisateur.class))).thenReturn(agent);

        UtilisateurReponse resultat = utilisateurService.creer(requete);

        assertThat(resultat).isNotNull();
        ArgumentCaptor<Utilisateur> captor = ArgumentCaptor.forClass(Utilisateur.class);
        verify(utilisateurRepository).save(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo(Role.AGENT);
        assertThat(captor.getValue().getAgence()).isEqualTo(agence);
    }

    @Test
    void doit_refuser_la_creation_dun_compte_admin() {
        CreationUtilisateurRequete requete = new CreationUtilisateurRequete(
                "Renard", "Sophie", "sophie.renard@transitea.fr",
                "+33676543210", "motdepasse123", Role.ADMIN, 1L);

        assertThatThrownBy(() -> utilisateurService.creer(requete))
                .isInstanceOf(ErreurMetier.class)
                .hasMessageContaining("ADMIN");

        verify(utilisateurRepository, never()).save(any());
    }

    @Test
    void doit_lancer_erreur_metier_quand_email_deja_utilise() {
        CreationUtilisateurRequete requete = new CreationUtilisateurRequete(
                "Renard", "Sophie", "louis.girard@transitea.fr",
                "+33676543210", "motdepasse123", Role.AGENT, 1L);

        when(utilisateurRepository.existsByEmail(requete.email())).thenReturn(true);

        assertThatThrownBy(() -> utilisateurService.creer(requete))
                .isInstanceOf(ErreurMetier.class)
                .hasMessageContaining("email");

        verify(utilisateurRepository, never()).save(any());
    }

    @Test
    void doit_lancer_exception_quand_agence_inexistante_a_la_creation() {
        CreationUtilisateurRequete requete = new CreationUtilisateurRequete(
                "Renard", "Sophie", "sophie.renard@transitea.fr",
                "+33676543210", "motdepasse123", Role.AGENT, 99L);

        when(utilisateurRepository.existsByEmail(requete.email())).thenReturn(false);
        when(utilisateurRepository.existsByTelephone(requete.telephone())).thenReturn(false);
        when(agenceRepository.findByIdAndSupprimeFalse(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> utilisateurService.creer(requete))
                .isInstanceOf(EntiteNonTrouveeException.class);
    }

    // --- mettreAJourStatut ---

    @Test
    void doit_desactiver_un_compte() {
        when(utilisateurRepository.findByIdAndSupprimeFalse(10L)).thenReturn(Optional.of(agent));
        when(utilisateurRepository.save(any(Utilisateur.class))).thenReturn(agent);

        utilisateurService.mettreAJourStatut(10L, StatutUtilisateur.INACTIF);

        ArgumentCaptor<Utilisateur> captor = ArgumentCaptor.forClass(Utilisateur.class);
        verify(utilisateurRepository).save(captor.capture());
        assertThat(captor.getValue().getStatut()).isEqualTo(StatutUtilisateur.INACTIF);
    }

    @Test
    void doit_lancer_exception_quand_utilisateur_inexistant() {
        when(utilisateurRepository.findByIdAndSupprimeFalse(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> utilisateurService.mettreAJourStatut(99L, StatutUtilisateur.INACTIF))
                .isInstanceOf(EntiteNonTrouveeException.class);
    }
}
