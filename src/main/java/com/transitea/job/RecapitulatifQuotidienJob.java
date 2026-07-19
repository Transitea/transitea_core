package com.transitea.job;

import com.transitea.entity.Agence;
import com.transitea.entity.Utilisateur;
import com.transitea.entity.enums.Role;
import com.transitea.entity.enums.StatutColis;
import com.transitea.repository.AgenceRepository;
import com.transitea.repository.ColisRepository;
import com.transitea.repository.MiseAJourStatutRepository;
import com.transitea.repository.UtilisateurRepository;
import jakarta.mail.internet.MimeMessage;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Envoie chaque jour a 18h00 un recapitulatif d'activite par e-mail aux
 * responsables (OPERATEUR) de chaque agence.
 */
@Component
public class RecapitulatifQuotidienJob implements Job {

    private static final Logger journal = LoggerFactory.getLogger(RecapitulatifQuotidienJob.class);

    private final AgenceRepository agenceRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final ColisRepository colisRepository;
    private final MiseAJourStatutRepository miseAJourStatutRepository;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String expediteurEmail;

    public RecapitulatifQuotidienJob(
            AgenceRepository agenceRepository,
            UtilisateurRepository utilisateurRepository,
            ColisRepository colisRepository,
            MiseAJourStatutRepository miseAJourStatutRepository,
            JavaMailSender mailSender) {
        this.agenceRepository = agenceRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.colisRepository = colisRepository;
        this.miseAJourStatutRepository = miseAJourStatutRepository;
        this.mailSender = mailSender;
    }

    @Override
    public void execute(JobExecutionContext context) {
        LocalDateTime debutJournee = LocalDate.now().atStartOfDay();
        LocalDateTime maintenant = LocalDateTime.now();

        List<Agence> agences = agenceRepository.findBySupprimeFalse();
        journal.info("Demarrage du recapitulatif quotidien pour {} agence(s)", agences.size());

        for (Agence agence : agences) {
            List<Utilisateur> operateurs =
                    utilisateurRepository.findByAgenceAndRoleAndSupprimeFalse(agence, Role.OPERATEUR);

            if (operateurs.isEmpty()) {
                continue;
            }

            long nbEnregistres = colisRepository.countByAgenceOrigineAndDateCreationBetweenAndSupprimeFalse(
                    agence, debutJournee, maintenant);
            long nbRetires = miseAJourStatutRepository.countByAgenceRetraitAndStatutAndDateCreationBetween(
                    agence, StatutColis.RETIRE, debutJournee, maintenant);

            String corpsHtml = construireCorpsHtml(agence, nbEnregistres, nbRetires);

            for (Utilisateur operateur : operateurs) {
                envoyerRecap(operateur.getEmail(), agence.getNom(), corpsHtml);
            }
        }
    }

    private void envoyerRecap(String destinataire, String nomAgence, String corpsHtml) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(expediteurEmail);
            helper.setTo(destinataire);
            helper.setSubject("Recapitulatif quotidien - " + nomAgence);
            helper.setText(corpsHtml, true);

            mailSender.send(message);
            journal.info("Recapitulatif envoye a {} pour l'agence {}", destinataire, nomAgence);

        } catch (Exception e) {
            journal.error("Echec envoi recapitulatif a {} pour l'agence {} : {}",
                    destinataire, nomAgence, e.getMessage());
        }
    }

    private String construireCorpsHtml(Agence agence, long nbEnregistres, long nbRetires) {
        return """
                <!DOCTYPE html>
                <html lang="fr">
                <body style="font-family: Arial, sans-serif; color: #333; max-width: 600px; margin: 0 auto;">
                  <div style="background-color: #1a56db; padding: 20px; text-align: center;">
                    <h1 style="color: white; margin: 0;">Transitea</h1>
                    <p style="color: #e0eaff; margin: 5px 0 0;">Recapitulatif quotidien</p>
                  </div>
                  <div style="padding: 30px;">
                    <p>Bonjour,</p>
                    <p>Voici le recapitulatif de l'activite du jour pour <strong>%s</strong> :</p>
                    <ul>
                      <li>Colis enregistres : <strong>%d</strong></li>
                      <li>Colis retires : <strong>%d</strong></li>
                    </ul>
                    <p style="color: #666; font-size: 12px;">
                      Ce message est genere automatiquement, merci de ne pas y repondre.
                    </p>
                  </div>
                </body>
                </html>
                """.formatted(agence.getNom(), nbEnregistres, nbRetires);
    }
}
