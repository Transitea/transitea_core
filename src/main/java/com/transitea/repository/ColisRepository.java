package com.transitea.repository;

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

    Optional<Colis> findByTransporteurAndLocalIdAndSupprimeFalse(
            Utilisateur transporteur, Long localId);

    Page<Colis> findByTransporteurAndSupprimeFalse(Utilisateur transporteur, Pageable pageable);

    Page<Colis> findByTransporteurAndStatutActuelAndSupprimeFalse(
            Utilisateur transporteur, StatutColis statut, Pageable pageable);

    Page<Colis> findBySupprimeFalse(Pageable pageable);

    Page<Colis> findByStatutActuelAndSupprimeFalse(StatutColis statut, Pageable pageable);

    @Query("SELECT c FROM Colis c WHERE c.transporteur = :transporteur " +
           "AND c.supprime = false " +
           "AND (LOWER(c.destinataireNom) LIKE LOWER(CONCAT('%', :recherche, '%')) " +
           "OR LOWER(c.codeTracking) LIKE LOWER(CONCAT('%', :recherche, '%')))")
    Page<Colis> rechercherParTransporteur(
            @Param("transporteur") Utilisateur transporteur,
            @Param("recherche") String recherche,
            Pageable pageable);

    @Query("SELECT c FROM Colis c WHERE c.supprime = false " +
           "AND (LOWER(c.destinataireNom) LIKE LOWER(CONCAT('%', :recherche, '%')) " +
           "OR LOWER(c.codeTracking) LIKE LOWER(CONCAT('%', :recherche, '%')))")
    Page<Colis> rechercherTous(@Param("recherche") String recherche, Pageable pageable);

    List<Colis> findByTransporteurAndDateCreationBetweenAndSupprimeFalse(
            Utilisateur transporteur, LocalDateTime debut, LocalDateTime fin);

    long countByTransporteurAndStatutActuelAndSupprimeFalse(
            Utilisateur transporteur, StatutColis statut);

    long countByStatutActuelAndSupprimeFalse(StatutColis statut);

    @Query("SELECT c FROM Colis c WHERE c.supprime = false " +
           "AND c.dateCreation < :dateLimite")
    List<Colis> trouverColisAArchiver(@Param("dateLimite") LocalDateTime dateLimite);
}
