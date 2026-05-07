package com.transitea.service.impl;

import com.transitea.entity.Colis;
import com.transitea.entity.Notification;
import com.transitea.entity.enums.StatutColis;
import com.transitea.entity.enums.StatutNotification;
import com.transitea.entity.enums.TypeCanal;
import com.transitea.repository.NotificationRepository;
import com.transitea.service.NotificationService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class NotificationServiceImpl implements NotificationService {

    private static final Logger journal = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final JavaMailSender mailSender;
    private final NotificationRepository notificationRepository;

    @Value("${spring.mail.username:}")
    private String expediteurEmail;

    @Value("${application.base-url:http://localhost:8080}")
    private String baseUrl;

    public NotificationServiceImpl(
            JavaMailSender mailSender,
            NotificationRepository notificationRepository) {
        this.mailSender = mailSender;
        this.notificationRepository = notificationRepository;
    }

    @Override
    @Async
    public void notifierChangementStatut(Colis colis, StatutColis ancienStatut) {
        String destinataireEmail = colis.getDestinataireEmail();

        if (destinataireEmail == null || destinataireEmail.isBlank()) {
            journal.debug("Pas de notification : email destinataire absent pour le colis {}",
                    colis.getCodeTracking());
            return;
        }

        Notification notification = Notification.builder()
                .colis(colis)
                .destinataire(destinataireEmail)
                .typeCanal(TypeCanal.EMAIL)
                .message(construireMessage(colis, ancienStatut))
                .build();

        try {
            envoyerEmail(
                    destinataireEmail,
                    construireSujet(colis),
                    construireCorpsHtml(colis, ancienStatut)
            );

            notification.setStatut(StatutNotification.ENVOYE);
            notification.setNbTentatives(1);
            journal.info("Notification email envoyee pour le colis {} a {}",
                    colis.getCodeTracking(), destinataireEmail);

        } catch (Exception e) {
            notification.setStatut(StatutNotification.ECHEC);
            notification.setNbTentatives(1);
            journal.error("Echec envoi email pour le colis {} : {}",
                    colis.getCodeTracking(), e.getMessage());
        }

        notificationRepository.save(notification);
    }

    private void envoyerEmail(String destinataire, String sujet, String corpsHtml)
            throws MessagingException {

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(expediteurEmail);
        helper.setTo(destinataire);
        helper.setSubject(sujet);
        helper.setText(corpsHtml, true);

        mailSender.send(message);
    }

    private String construireSujet(Colis colis) {
        return String.format("Votre colis %s - Statut mis a jour : %s",
                colis.getCodeTracking(),
                formaterStatut(colis.getStatutActuel()));
    }

    private String construireMessage(Colis colis, StatutColis ancienStatut) {
        return String.format("Colis %s : %s -> %s",
                colis.getCodeTracking(),
                ancienStatut != null ? ancienStatut.name() : "CREATION",
                colis.getStatutActuel().name());
    }

    private String construireCorpsHtml(Colis colis, StatutColis ancienStatut) {
        String lienTracking = baseUrl + "/tracking/" + colis.getCodeTracking();
        String ancienStatutLabel = ancienStatut != null
                ? formaterStatut(ancienStatut)
                : "Nouveau colis";

        return """
                <!DOCTYPE html>
                <html lang="fr">
                <body style="font-family: Arial, sans-serif; color: #333; max-width: 600px; margin: 0 auto;">

                  <div style="background-color: #1a56db; padding: 20px; text-align: center;">
                    <h1 style="color: white; margin: 0;">Transitea</h1>
                    <p style="color: #e0eaff; margin: 5px 0 0;">Suivi de votre colis</p>
                  </div>

                  <div style="padding: 30px;">
                    <p>Bonjour <strong>%s</strong>,</p>
                    <p>Votre colis a ete mis a jour.</p>

                    <div style="background-color: #f4f7ff; border-left: 4px solid #1a56db;
                                padding: 15px; margin: 20px 0; border-radius: 4px;">
                      <p style="margin: 0 0 8px;"><strong>Reference :</strong> %s</p>
                      <p style="margin: 0 0 8px;"><strong>Statut precedent :</strong> %s</p>
                      <p style="margin: 0;"><strong>Nouveau statut :</strong>
                        <span style="color: #1a56db; font-weight: bold;">%s</span>
                      </p>
                    </div>

                    <div style="text-align: center; margin: 30px 0;">
                      <a href="%s"
                         style="background-color: #1a56db; color: white; padding: 12px 24px;
                                text-decoration: none; border-radius: 4px; font-weight: bold;">
                        Suivre mon colis
                      </a>
                    </div>

                    <p style="color: #666; font-size: 12px;">
                      Ce message est genere automatiquement, merci de ne pas y repondre.
                    </p>
                  </div>

                </body>
                </html>
                """.formatted(
                colis.getDestinataireNom(),
                colis.getCodeTracking(),
                ancienStatutLabel,
                formaterStatut(colis.getStatutActuel()),
                lienTracking
        );
    }

    private String formaterStatut(StatutColis statut) {
        return switch (statut) {
            case ENREGISTRE -> "Enregistre";
            case PRIS_EN_CHARGE -> "Pris en charge";
            case EN_TRANSIT -> "En transit";
            case ARRIVE_DEPOT -> "Arrive au depot";
            case EN_LIVRAISON -> "En cours de livraison";
            case LIVRE -> "Livre";
            case REFUSE -> "Refuse";
            case RETOUR_EXPEDITEUR -> "Retour expediteur";
            case PASSAGE_FRONTIERE -> "Passage frontiere";
            case EN_DOUANE -> "En douane";
        };
    }
}
