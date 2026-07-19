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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobExecutionContext;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecapitulatifQuotidienJobTest {

    @Mock
    private AgenceRepository agenceRepository;

    @Mock
    private UtilisateurRepository utilisateurRepository;

    @Mock
    private ColisRepository colisRepository;

    @Mock
    private MiseAJourStatutRepository miseAJourStatutRepository;

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private JobExecutionContext jobExecutionContext;

    @InjectMocks
    private RecapitulatifQuotidienJob job;

    private Agence agence;
    private Utilisateur operateur;

    @BeforeEach
    void initialiser() {
        ReflectionTestUtils.setField(job, "expediteurEmail", "noreply@transitea.cd");

        agence = Agence.builder().nom("Agence Kinshasa").ville("Kinshasa").build();
        agence.setId(1L);

        operateur = Utilisateur.builder()
                .nom("Mutombo")
                .prenom("Pierre")
                .email("operateur@transitea.cd")
                .role(Role.OPERATEUR)
                .agence(agence)
                .build();
        operateur.setId(1L);
    }

    @Test
    void doit_envoyer_un_recap_a_chaque_operateur_de_chaque_agence() throws Exception {
        when(agenceRepository.findBySupprimeFalse()).thenReturn(List.of(agence));
        when(utilisateurRepository.findByAgenceAndRoleAndSupprimeFalse(agence, Role.OPERATEUR))
                .thenReturn(List.of(operateur));
        when(colisRepository.countByAgenceOrigineAndDateCreationBetweenAndSupprimeFalse(
                any(), any(), any())).thenReturn(5L);
        when(miseAJourStatutRepository.countByAgenceRetraitAndStatutAndDateCreationBetween(
                any(), any(StatutColis.class), any(), any())).thenReturn(3L);

        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        job.execute(jobExecutionContext);

        verify(mailSender).send(mimeMessage);
    }

    @Test
    void doit_ignorer_agence_sans_operateur() {
        when(agenceRepository.findBySupprimeFalse()).thenReturn(List.of(agence));
        when(utilisateurRepository.findByAgenceAndRoleAndSupprimeFalse(agence, Role.OPERATEUR))
                .thenReturn(List.of());

        job.execute(jobExecutionContext);

        verify(mailSender, never()).createMimeMessage();
        verify(colisRepository, never())
                .countByAgenceOrigineAndDateCreationBetweenAndSupprimeFalse(any(), any(), any());
    }

    @Test
    void ne_doit_pas_planter_quand_envoi_email_echoue() {
        when(agenceRepository.findBySupprimeFalse()).thenReturn(List.of(agence));
        when(utilisateurRepository.findByAgenceAndRoleAndSupprimeFalse(agence, Role.OPERATEUR))
                .thenReturn(List.of(operateur));
        when(colisRepository.countByAgenceOrigineAndDateCreationBetweenAndSupprimeFalse(
                any(), any(), any())).thenReturn(0L);
        when(miseAJourStatutRepository.countByAgenceRetraitAndStatutAndDateCreationBetween(
                any(), any(StatutColis.class), any(), any())).thenReturn(0L);
        when(mailSender.createMimeMessage()).thenThrow(new RuntimeException("SMTP indisponible"));

        job.execute(jobExecutionContext);

        verify(mailSender, never()).send(any(MimeMessage.class));
    }
}
