package com.transitea.service.impl;

import com.transitea.entity.Colis;
import com.transitea.entity.Utilisateur;
import com.transitea.entity.enums.Role;
import com.transitea.repository.ColisRepository;
import com.transitea.service.ExportService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ExportServiceImpl implements ExportService {

    private static final DateTimeFormatter FORMATEUR_DATE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final String EN_TETE_CSV =
            "Code Tracking,Expediteur,Tel Expediteur,Destinataire,Tel Destinataire," +
            "Ville,Description,Poids (kg),Statut,Date creation\n";

    private final ColisRepository colisRepository;

    public ExportServiceImpl(ColisRepository colisRepository) {
        this.colisRepository = colisRepository;
    }

    @Override
    public byte[] exporterCsvColis(Utilisateur utilisateur, LocalDateTime debut, LocalDateTime fin) {
        List<Colis> colisList;

        if (utilisateur.getRole() == Role.ADMIN || utilisateur.getRole() == Role.OPERATEUR) {
            colisList = colisRepository.findBySupprimeFalse(
                    org.springframework.data.domain.Pageable.unpaged()).getContent();
            colisList = colisList.stream()
                    .filter(c -> !c.getDateCreation().isBefore(debut)
                              && !c.getDateCreation().isAfter(fin))
                    .toList();
        } else {
            colisList = colisRepository.findByTransporteurAndDateCreationBetweenAndSupprimeFalse(
                    utilisateur, debut, fin);
        }

        StringBuilder csv = new StringBuilder(EN_TETE_CSV);
        for (Colis c : colisList) {
            csv.append(echapper(c.getCodeTracking())).append(',')
               .append(echapper(c.getExpediteurNom())).append(',')
               .append(echapper(c.getExpediteurTelephone())).append(',')
               .append(echapper(c.getDestinataireNom())).append(',')
               .append(echapper(c.getDestinataireTelephone())).append(',')
               .append(echapper(c.getDestinataireVille())).append(',')
               .append(echapper(c.getDescription())).append(',')
               .append(c.getPoids() != null ? c.getPoids().toPlainString() : "").append(',')
               .append(c.getStatutActuel().name()).append(',')
               .append(c.getDateCreation() != null
                       ? c.getDateCreation().format(FORMATEUR_DATE) : "")
               .append('\n');
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String echapper(String valeur) {
        if (valeur == null) return "";
        if (valeur.contains(",") || valeur.contains("\"") || valeur.contains("\n")) {
            return "\"" + valeur.replace("\"", "\"\"") + "\"";
        }
        return valeur;
    }
}
