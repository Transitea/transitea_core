package com.transitea.entity;

import com.transitea.entity.enums.StatutNotification;
import com.transitea.entity.enums.TypeCanal;
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
@Table(name = "notification")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification extends EntiteBase {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "colis_id", nullable = false)
    private Colis colis;

    @Column(nullable = false)
    private String destinataire;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_canal", nullable = false)
    private TypeCanal typeCanal;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StatutNotification statut = StatutNotification.EN_ATTENTE;

    @Column(name = "nb_tentatives", nullable = false)
    @Builder.Default
    private Integer nbTentatives = 0;
}
