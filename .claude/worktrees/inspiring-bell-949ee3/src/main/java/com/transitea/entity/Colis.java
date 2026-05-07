package com.transitea.entity;

import com.transitea.entity.enums.StatutColis;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "colis")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Colis extends EntiteBase {

    @Column(nullable = false, unique = true, updatable = false)
    @Builder.Default
    private String uuid = UUID.randomUUID().toString();

    @Column(name = "code_tracking", nullable = false, unique = true)
    private String codeTracking;

    @Column(name = "qr_code_path")
    private String qrCodePath;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transporteur_id", nullable = false)
    private Utilisateur transporteur;

    @Column(name = "expediteur_nom", nullable = false)
    private String expediteurNom;

    @Column(name = "expediteur_telephone")
    private String expediteurTelephone;

    @Column(name = "expediteur_email")
    private String expediteurEmail;

    @Column(name = "destinataire_nom", nullable = false)
    private String destinataireNom;

    @Column(name = "destinataire_telephone")
    private String destinataireTelephone;

    @Column(name = "destinataire_email")
    private String destinataireEmail;

    @Column(name = "destinataire_adresse", columnDefinition = "TEXT")
    private String destinataireAdresse;

    @Column(name = "destinataire_ville")
    private String destinataireVille;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(precision = 10, scale = 3)
    private BigDecimal poids;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut_actuel", nullable = false)
    @Builder.Default
    private StatutColis statutActuel = StatutColis.ENREGISTRE;

    @Column(name = "local_id")
    private Long localId;

    @Version
    @Column(nullable = false)
    @Builder.Default
    private Integer version = 0;

    @Column(nullable = false)
    @Builder.Default
    private Boolean supprime = false;
}
