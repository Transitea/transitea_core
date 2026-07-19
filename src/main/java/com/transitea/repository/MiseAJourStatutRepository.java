package com.transitea.repository;

import com.transitea.entity.Agence;
import com.transitea.entity.Colis;
import com.transitea.entity.MiseAJourStatut;
import com.transitea.entity.enums.StatutColis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MiseAJourStatutRepository extends JpaRepository<MiseAJourStatut, Long> {

    List<MiseAJourStatut> findByColisOrderByDateCreationAsc(Colis colis);

    List<MiseAJourStatut> findByColisOrderByDateCreationDesc(Colis colis);

    @Query("SELECT COUNT(m) FROM MiseAJourStatut m WHERE m.colis.agenceRetrait = :agence " +
           "AND m.statut = :statut AND m.dateCreation BETWEEN :debut AND :fin")
    long countByAgenceRetraitAndStatutAndDateCreationBetween(
            @Param("agence") Agence agence,
            @Param("statut") StatutColis statut,
            @Param("debut") LocalDateTime debut,
            @Param("fin") LocalDateTime fin);
}
