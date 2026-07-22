package com.transitea.service.impl;

import com.transitea.entity.Agence;
import com.transitea.entity.Colis;
import com.transitea.entity.Notification;
import com.transitea.entity.Utilisateur;
import com.transitea.entity.enums.Role;
import com.transitea.entity.enums.StatutColis;
import com.transitea.entity.enums.StatutNotification;
import com.transitea.entity.enums.TypeCanal;
import com.transitea.exception.AccesNonAutoriseException;
import com.transitea.repository.NotificationRepository;
import com.transitea.service.WhatsAppService;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private WhatsAppService whatsAppService;

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private Colis colisAvecEmail;
    private Colis colisAvecTelephoneEtEmail;
    private Colis colisSansContact;

    @BeforeEach
    void initialiser() {
        ReflectionTestUtils.setField(notificationService, "expediteurEmail", "noreply@transitea.cd");
        ReflectionTestUtils.setField(notificationService, "baseUrl", "http://localhost:8080");

        Utilisateur agent = Utilisateur.builder()
                .nom("Lumbu")
                .prenom("Louange")
                .email("louange@transitea.cd")
                .role(Role.AGENT)
                .build();
        agent.setId(1L);

        colisAvecEmail = Colis.builder()
                .codeTracking("TRA-2026-ABC123")
                .creePar(agent)
                .expediteurNom("Jean Dupont")
                .destinataireNom("Marie Martin")
                .destinataireEmail("marie@example.com")
                .poids(new BigDecimal("2.500"))
                .statutActuel(StatutColis.ARRIVE_AGENCE)
                .build();
        colisAvecEmail.setId(1L);

        colisAvecTelephoneEtEmail = Colis.builder()
                .codeTracking("TRA-2026-DEF456")
                .creePar(agent)
                .expediteurNom("Jean Dupont")
                .destinataireNom("Marie Martin")
                .destinataireTelephone("+243900000011")
                .destinataireEmail("marie@example.com")
                .poids(new BigDecimal("2.500"))
                .statutActuel(StatutColis.ARRIVE_AGENCE)
                .build();
        colisAvecTelephoneEtEmail.setId(3L);

        colisSansContact = Colis.builder()
                .codeTracking("TRA-2026-XYZ999")
                .creePar(agent)
                .expediteurNom("Jean Dupont")
                .destinataireNom("Paul Dupont")
                .poids(new BigDecimal("1.000"))
                .statutActuel(StatutColis.ARRIVE_AGENCE)
                .build();
        colisSansContact.setId(2L);
    }

    // --- WhatsApp prioritaire ---

    @Test
    void doit_privilegier_whatsapp_quand_telephone_present_et_envoi_reussi() {
        when(whatsAppService.envoyerMessage(anyString(), anyString())).thenReturn(true);

        notificationService.notifierChangementStatut(colisAvecTelephoneEtEmail, StatutColis.ENREGISTRE);

        verify(whatsAppService).envoyerMessage(eq("+243900000011"), anyString());
        verify(mailSender, never()).createMimeMessage();

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getTypeCanal()).isEqualTo(TypeCanal.WHATSAPP);
        assertThat(captor.getValue().getStatut()).isEqualTo(StatutNotification.ENVOYE);
        assertThat(captor.getValue().getDestinataire()).isEqualTo("+243900000011");
    }

    @Test
    void doit_replier_sur_email_quand_whatsapp_echoue() {
        when(whatsAppService.envoyerMessage(anyString(), anyString())).thenReturn(false);
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        notificationService.notifierChangementStatut(colisAvecTelephoneEtEmail, StatutColis.ENREGISTRE);

        verify(whatsAppService).envoyerMessage(anyString(), anyString());
        verify(mailSender).send(any(MimeMessage.class));

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getTypeCanal()).isEqualTo(TypeCanal.EMAIL);
        assertThat(captor.getValue().getDestinataire()).isEqualTo("marie@example.com");
    }

    @Test
    void doit_ne_pas_appeler_whatsapp_quand_telephone_absent() {
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        notificationService.notifierChangementStatut(colisAvecEmail, StatutColis.ENREGISTRE);

        verify(whatsAppService, never()).envoyerMessage(anyString(), anyString());
        verify(mailSender).send(any(MimeMessage.class));
    }

    // --- E-mail (repli) ---

    @Test
    void doit_envoyer_email_et_sauvegarder_notification_envoye_quand_email_present() throws Exception {
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        notificationService.notifierChangementStatut(colisAvecEmail, StatutColis.ENREGISTRE);

        verify(mailSender).send(any(MimeMessage.class));

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());

        Notification notif = captor.getValue();
        assertThat(notif.getStatut()).isEqualTo(StatutNotification.ENVOYE);
        assertThat(notif.getTypeCanal()).isEqualTo(TypeCanal.EMAIL);
        assertThat(notif.getDestinataire()).isEqualTo("marie@example.com");
        assertThat(notif.getNbTentatives()).isEqualTo(1);
    }

    @Test
    void doit_ne_rien_envoyer_quand_ni_telephone_ni_email() {
        notificationService.notifierChangementStatut(colisSansContact, StatutColis.ENREGISTRE);

        verify(mailSender, never()).createMimeMessage();
        verify(whatsAppService, never()).envoyerMessage(anyString(), anyString());
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void doit_sauvegarder_notification_echec_quand_envoi_echoue() {
        when(mailSender.createMimeMessage()).thenThrow(new RuntimeException("SMTP indisponible"));

        notificationService.notifierChangementStatut(colisAvecEmail, StatutColis.ENREGISTRE);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());

        Notification notif = captor.getValue();
        assertThat(notif.getStatut()).isEqualTo(StatutNotification.ECHEC);
        assertThat(notif.getNbTentatives()).isEqualTo(1);
    }

    @Test
    void doit_inclure_lien_tracking_dans_le_message() {
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        notificationService.notifierChangementStatut(colisAvecEmail, StatutColis.ENREGISTRE);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());

        assertThat(captor.getValue().getMessage())
                .contains("TRA-2026-ABC123");
    }

    @Test
    void doit_ignorer_statut_en_transit() {
        colisAvecEmail.setStatutActuel(StatutColis.EN_TRANSIT);

        notificationService.notifierChangementStatut(colisAvecEmail, StatutColis.ENREGISTRE);

        verify(mailSender, never()).createMimeMessage();
        verify(whatsAppService, never()).envoyerMessage(anyString(), anyString());
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void doit_notifier_expediteur_quand_colis_retire() {
        colisAvecEmail.setExpediteurEmail("jean@example.com");
        colisAvecEmail.setStatutActuel(StatutColis.RETIRE);
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        notificationService.notifierChangementStatut(colisAvecEmail, StatutColis.ARRIVE_AGENCE);

        // 1 notification destinataire + 1 notification expediteur
        verify(notificationRepository, org.mockito.Mockito.times(2)).save(any(Notification.class));
    }

    @Test
    void doit_deduire_la_cible_expediteur_quand_le_contact_correspond_a_lexpediteur() {
        colisAvecEmail.setExpediteurEmail("jean@example.com");
        Notification notification = Notification.builder()
                .colis(colisAvecEmail)
                .destinataire("jean@example.com")
                .typeCanal(TypeCanal.EMAIL)
                .message("Colis retire")
                .statut(StatutNotification.ENVOYE)
                .nbTentatives(1)
                .build();
        notification.setId(10L);

        Utilisateur admin = Utilisateur.builder().role(Role.ADMIN).build();
        Pageable pageable = PageRequest.of(0, 20);
        when(notificationRepository.findAllByOrderByDateCreationDesc(pageable))
                .thenReturn(new PageImpl<>(java.util.List.of(notification)));

        var resultat = notificationService.lister(admin, pageable);

        assertThat(resultat.contenu()).hasSize(1);
        assertThat(resultat.contenu().get(0).cible()).isEqualTo("EXPEDITEUR");
        assertThat(resultat.contenu().get(0).codeTracking()).isEqualTo("TRA-2026-ABC123");
    }

    @Test
    void doit_deduire_la_cible_destinataire_par_defaut() {
        Notification notification = Notification.builder()
                .colis(colisAvecEmail)
                .destinataire("marie@example.com")
                .typeCanal(TypeCanal.EMAIL)
                .message("Colis enregistre")
                .statut(StatutNotification.ENVOYE)
                .nbTentatives(1)
                .build();
        notification.setId(11L);

        Agence agence = Agence.builder().nom("Agence Paris").build();
        agence.setId(1L);
        Utilisateur agent = Utilisateur.builder().role(Role.AGENT).agence(agence).build();
        Pageable pageable = PageRequest.of(0, 20);
        when(notificationRepository.findByAgenceOrderByDateCreationDesc(agence, pageable))
                .thenReturn(new PageImpl<>(java.util.List.of(notification)));

        var resultat = notificationService.lister(agent, pageable);

        assertThat(resultat.contenu().get(0).cible()).isEqualTo("DESTINATAIRE");
    }

    @Test
    void doit_refuser_un_utilisateur_sans_agence() {
        Utilisateur sansAgence = Utilisateur.builder().role(Role.AGENT).build();
        Pageable pageable = PageRequest.of(0, 20);

        assertThatThrownBy(() -> notificationService.lister(sansAgence, pageable))
                .isInstanceOf(AccesNonAutoriseException.class);
    }
}
