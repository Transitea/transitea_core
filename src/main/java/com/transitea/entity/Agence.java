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

import java.util.UUID;

@Entity
@Table(name = "agence")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Agence extends EntiteBase {

    @Column(nullable = false, unique = true, updatable = false)
    @Builder.Default
    private String uuid = UUID.randomUUID().toString();

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String ville;

    @Column(columnDefinition = "TEXT")
    private String adresse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enseigne_id", nullable = false)
    private Enseigne enseigne;

    @Column(nullable = false)
    @Builder.Default
    private Boolean supprime = false;
}
