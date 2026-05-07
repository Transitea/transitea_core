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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "mise_a_jour_statut")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MiseAJourStatut extends EntiteBase {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "colis_id", nullable = false)
    private Colis colis;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutColis statut;

    @Enumerated(EnumType.STRING)
    @Column(name = "ancien_statut")
    private StatutColis ancienStatut;

    @Column
    private String localisation;

    @Column(columnDefinition = "TEXT")
    private String commentaire;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_id")
    private Utilisateur utilisateur;
}
