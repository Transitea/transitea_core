package com.transitea.repository;

import com.transitea.entity.Agence;
import com.transitea.entity.Colis;
import com.transitea.entity.Utilisateur;
import com.transitea.entity.enums.StatutColis;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ColisRepository extends JpaRepository<Colis, Long> {

    Optional<Colis> findByCodeTrackingAndSupprimeFalse(String codeTracking);

    Optional<Colis> findByUuidAndSupprimeFalse(String uuid);

    Optional<Colis> findByCreeParAndLocalIdAndSupprimeFalse(
            Utilisateur creePar, Long localId);

    Page<Colis> findBySupprimeFalse(Pageable pageable);

    Page<Colis> findByStatutActuelAndSupprimeFalse(StatutColis statut, Pageable pageable);

    @Query("SELECT c FROM Colis c WHERE c.supprime = false " +
           "AND (c.agenceOrigine = :agence OR c.agenceRetrait = :agence)")
    Page<Colis> findByAgenceAndSupprimeFalse(@Param("agence") Agence agence, Pageable pageable);

    @Query("SELECT c FROM Colis c WHERE c.supprime = false " +
           "AND (c.agenceOrigine = :agence OR c.agenceRetrait = :agence) " +
           "AND c.statutActuel = :statut")
    Page<Colis> findByAgenceAndStatutActuelAndSupprimeFalse(
            @Param("agence") Agence agence,
            @Param("statut") StatutColis statut,
            Pageable pageable);

    @Query("SELECT c FROM Colis c WHERE c.supprime = false " +
           "AND (c.agenceOrigine = :agence OR c.agenceRetrait = :agence) " +
           "AND (LOWER(c.destinataireNom) LIKE LOWER(CONCAT('%', :recherche, '%')) " +
           "OR LOWER(c.codeTracking) LIKE LOWER(CONCAT('%', :recherche, '%')))")
    Page<Colis> rechercherParAgence(
            @Param("agence") Agence agence,
            @Param("recherche") String recherche,
            Pageable pageable);

    @Query("SELECT c FROM Colis c WHERE c.supprime = false " +
           "AND (LOWER(c.destinataireNom) LIKE LOWER(CONCAT('%', :recherche, '%')) " +
           "OR LOWER(c.codeTracking) LIKE LOWER(CONCAT('%', :recherche, '%')))")
    Page<Colis> rechercherTous(@Param("recherche") String recherche, Pageable pageable);

    List<Colis> findByCreeParAndDateCreationBetweenAndSupprimeFalse(
            Utilisateur creePar, LocalDateTime debut, LocalDateTime fin);

    @Query("SELECT COUNT(c) FROM Colis c WHERE c.supprime = false " +
           "AND (c.agenceOrigine = :agence OR c.agenceRetrait = :agence) " +
           "AND c.statutActuel = :statut")
    long countByAgenceAndStatutActuelAndSupprimeFalse(
            @Param("agence") Agence agence,
            @Param("statut") StatutColis statut);

    long countByStatutActuelAndSupprimeFalse(StatutColis statut);

    @Query("SELECT c FROM Colis c WHERE c.supprime = false " +
           "AND c.dateCreation < :dateLimite")
    List<Colis> trouverColisAArchiver(@Param("dateLimite") LocalDateTime dateLimite);
}
