package com.transitea.entity;

import com.transitea.entity.enums.PalierAbonnement;
import com.transitea.entity.enums.StatutEnseigne;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "enseigne")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Enseigne extends EntiteBase {

    @Column(nullable = false, unique = true, updatable = false)
    @Builder.Default
    private String uuid = UUID.randomUUID().toString();

    @Column(nullable = false)
    private String nom;

    @Enumerated(EnumType.STRING)
    @Column(name = "palier_abonnement", nullable = false)
    private PalierAbonnement palierAbonnement;

    @Column(name = "quota_colis_mois", nullable = false)
    private Integer quotaColisMois;

    @Column(name = "colis_mois_courant", nullable = false)
    @Builder.Default
    private Integer colisMoisCourant = 0;

    @Column(name = "date_debut_abonnement", nullable = false)
    private LocalDateTime dateDebutAbonnement;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StatutEnseigne statut = StatutEnseigne.ACTIF;

    @Column(nullable = false)
    @Builder.Default
    private Boolean supprime = false;
}
