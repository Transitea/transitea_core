package com.transitea.service.impl;

import com.transitea.entity.Colis;
import com.transitea.entity.Notification;
import com.transitea.entity.Utilisateur;
import com.transitea.entity.enums.Role;
import com.transitea.entity.enums.StatutColis;
import com.transitea.entity.enums.StatutNotification;
import com.transitea.entity.enums.TypeCanal;
import com.transitea.repository.NotificationRepository;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private Colis colisAvecEmail;
    private Colis colisSansEmail;

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

        colisSansEmail = Colis.builder()
                .codeTracking("TRA-2026-XYZ999")
                .creePar(agent)
                .expediteurNom("Jean Dupont")
                .destinataireNom("Paul Dupont")
                .poids(new BigDecimal("1.000"))
                .statutActuel(StatutColis.ARRIVE_AGENCE)
                .build();
        colisSansEmail.setId(2L);
    }

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
    void doit_ne_pas_envoyer_email_quand_destinataire_sans_email() {
        notificationService.notifierChangementStatut(colisSansEmail, StatutColis.ENREGISTRE);

        verify(mailSender, never()).createMimeMessage();
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
}
