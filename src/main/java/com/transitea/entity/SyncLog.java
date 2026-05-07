package com.transitea.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "sync_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SyncLog extends EntiteBase {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transporteur_id", nullable = false)
    private Utilisateur transporteur;

    @Column(name = "nb_colis_envoyes", nullable = false)
    @Builder.Default
    private Integer nbColisEnvoyes = 0;

    @Column(name = "nb_colis_reussis", nullable = false)
    @Builder.Default
    private Integer nbColisReussis = 0;

    @Column(name = "nb_colis_echecs", nullable = false)
    @Builder.Default
    private Integer nbColisEchecs = 0;

    @Column(name = "date_debut", nullable = false)
    private LocalDateTime dateDebut;

    @Column(name = "date_fin")
    private LocalDateTime dateFin;
}
