package com.transitea.job;

import com.transitea.entity.Enseigne;
import com.transitea.repository.EnseigneRepository;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Remet a zero le compteur mensuel de colis de chaque enseigne le 1er de
 * chaque mois a minuit.
 */
@Component
public class ReinitialisationQuotaMensuelJob implements Job {

    private static final Logger journal = LoggerFactory.getLogger(ReinitialisationQuotaMensuelJob.class);

    private final EnseigneRepository enseigneRepository;

    public ReinitialisationQuotaMensuelJob(EnseigneRepository enseigneRepository) {
        this.enseigneRepository = enseigneRepository;
    }

    @Override
    public void execute(JobExecutionContext context) {
        List<Enseigne> enseignes = enseigneRepository.findAll();

        for (Enseigne enseigne : enseignes) {
            enseigne.setColisMoisCourant(0);
        }

        enseigneRepository.saveAll(enseignes);
        journal.info("Quota mensuel reinitialise pour {} enseigne(s)", enseignes.size());
    }
}
