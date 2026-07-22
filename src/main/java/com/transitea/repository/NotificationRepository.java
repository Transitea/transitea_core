package com.transitea.repository;

import com.transitea.entity.Agence;
import com.transitea.entity.Colis;
import com.transitea.entity.Notification;
import com.transitea.entity.enums.StatutNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByColisAndStatut(Colis colis, StatutNotification statut);

    List<Notification> findByStatutAndNbTentativesLessThan(
            StatutNotification statut, int nbTentativesMax);

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.dateCreation < :dateLimite")
    int supprimerNotificationsAnciennes(@Param("dateLimite") LocalDateTime dateLimite);

    Page<Notification> findAllByOrderByDateCreationDesc(Pageable pageable);

    @Query("SELECT n FROM Notification n WHERE " +
           "(n.colis.agenceOrigine = :agence OR n.colis.agenceRetrait = :agence) " +
           "ORDER BY n.dateCreation DESC")
    Page<Notification> findByAgenceOrderByDateCreationDesc(@Param("agence") Agence agence, Pageable pageable);
}
