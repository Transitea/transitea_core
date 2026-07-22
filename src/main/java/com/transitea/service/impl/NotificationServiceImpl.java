package com.transitea.service.impl;

import com.transitea.dto.response.NotificationReponse;
import com.transitea.dto.response.ReponsePagee;
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
import com.transitea.service.NotificationService;
import com.transitea.service.WhatsAppService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
public class NotificationServiceImpl implements NotificationService {

    private static final Logger journal = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final JavaMailSender mailSender;
    private final WhatsAppService whatsAppService;
    private final NotificationRepository notificationRepository;

    @Value("${spring.mail.username:}")
    private String expediteurEmail;

    @Value("${application.base-url:http://localhost:8080}")
    private String baseUrl;

    public NotificationServiceImpl(
            JavaMailSender mailSender,
            WhatsAppService whatsAppService,
            NotificationRepository notificationRepository) {
        this.mailSender = mailSender;
        this.whatsAppService = whatsAppService;
        this.notificationRepository = notificationRepository;
    }

    @Override
    @Async
    public void notifierChangementStatut(Colis colis, StatutColis ancienStatut) {
        if (colis.getStatutActuel() == StatutColis.EN_TRANSIT) {
            journal.debug("Pas de notification pour le statut interne EN_TRANSIT (colis {})",
                    colis.getCodeTracking());
            return;
        }

        notifierDestinataire(colis, ancienStatut);

        if (colis.getStatutActuel() == StatutColis.RETIRE) {
            notifierExpediteur(colis);
        }
    }

    private void notifierDestinataire(Colis colis, StatutColis ancienStatut) {
        envoyerNotification(
                colis,
                colis.getDestinataireTelephone(),
                colis.getDestinataireEmail(),
                construireSujet(colis),
                construireMessage(colis, ancienStatut),
                construireCorpsHtml(colis, ancienStatut)
        );
    }

    private void notifierExpediteur(Colis colis) {
        envoyerNotification(
                colis,
                colis.getExpediteurTelephone(),
                colis.getExpediteurEmail(),
                "Votre colis " + colis.getCodeTracking() + " a ete retire",
                "Votre colis " + colis.getCodeTracking() + " a ete retire par le destinataire.",
                "<p>Bonjour,</p><p>Votre colis <strong>" + colis.getCodeTracking()
                        + "</strong> a bien ete retire par le destinataire.</p>"
        );
    }

    /**
     * WhatsApp en canal prioritaire, repli automatique sur e-mail si le
     * telephone est absent ou si l'envoi WhatsApp echoue.
     */
    private void envoyerNotification(
            Colis colis, String telephone, String email,
            String sujet, String messageTexte, String corpsHtml) {

        if ((telephone == null || telephone.isBlank()) && (email == null || email.isBlank())) {
            journal.debug("Pas de notification possible (ni telephone ni email) pour le colis {}",
                    colis.getCodeTracking());
            return;
        }

        if (telephone != null && !telephone.isBlank()
                && whatsAppService.envoyerMessage(telephone, messageTexte)) {
            enregistrerNotification(colis, telephone, TypeCanal.WHATSAPP, messageTexte, StatutNotification.ENVOYE);
            journal.info("Notification WhatsApp envoyee pour le colis {} a {}",
                    colis.getCodeTracking(), telephone);
            return;
        }

        if (email == null || email.isBlank()) {
            journal.debug("Pas de repli e-mail possible (email absent) pour le colis {}",
                    colis.getCodeTracking());
            return;
        }

        try {
            envoyerEmail(email, sujet, corpsHtml);
            enregistrerNotification(colis, email, TypeCanal.EMAIL, messageTexte, StatutNotification.ENVOYE);
            journal.info("Notification email envoyee pour le colis {} a {}",
                    colis.getCodeTracking(), email);

        } catch (Exception e) {
            enregistrerNotification(colis, email, TypeCanal.EMAIL, messageTexte, StatutNotification.ECHEC);
            journal.error("Echec envoi email pour le colis {} : {}",
                    colis.getCodeTracking(), e.getMessage());
        }
    }

    private void enregistrerNotification(
            Colis colis, String destinataire, TypeCanal canal, String message, StatutNotification statut) {

        Notification notification = Notification.builder()
                .colis(colis)
                .destinataire(destinataire)
                .typeCanal(canal)
                .message(message)
                .statut(statut)
                .nbTentatives(1)
                .build();

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
        // /suivi/{code} : page de suivi publique du frontend (CDC 8.3), pas l'API backend.
        String lienTracking = baseUrl + "/suivi/" + colis.getCodeTracking();
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
            case EN_TRANSIT -> "En transit";
            case ARRIVE_AGENCE -> "Arrive a l'agence de retrait";
            case RETIRE -> "Retire";
            case REFUSE -> "Refuse";
            case RETOUR_EXPEDITEUR -> "Retour expediteur";
        };
    }

    @Override
    @Transactional(readOnly = true)
    public ReponsePagee<NotificationReponse> lister(Utilisateur utilisateur, Pageable pageable) {
        Page<Notification> page = utilisateur.getRole() == Role.ADMIN
                ? notificationRepository.findAllByOrderByDateCreationDesc(pageable)
                : notificationRepository.findByAgenceOrderByDateCreationDesc(
                        agenceDeLUtilisateur(utilisateur), pageable);

        return ReponsePagee.depuis(page.map(this::versReponse));
    }

    private Agence agenceDeLUtilisateur(Utilisateur utilisateur) {
        if (utilisateur.getAgence() == null) {
            throw new AccesNonAutoriseException();
        }
        return utilisateur.getAgence();
    }

    /**
     * Aucun champ "cible" n'est persiste : on le deduit en comparant le contact
     * notifie a ceux de l'expediteur / du destinataire du colis.
     */
    private NotificationReponse versReponse(Notification notification) {
        Colis colis = notification.getColis();
        boolean estExpediteur =
                Objects.equals(notification.getDestinataire(), colis.getExpediteurTelephone())
                        || Objects.equals(notification.getDestinataire(), colis.getExpediteurEmail());

        return new NotificationReponse(
                notification.getId(),
                colis.getId(),
                colis.getCodeTracking(),
                notification.getDestinataire(),
                estExpediteur ? "EXPEDITEUR" : "DESTINATAIRE",
                notification.getTypeCanal(),
                notification.getMessage(),
                notification.getStatut(),
                notification.getNbTentatives(),
                notification.getDateCreation()
        );
    }
}
