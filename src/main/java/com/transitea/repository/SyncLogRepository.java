package com.transitea.repository;

import com.transitea.entity.SyncLog;
import com.transitea.entity.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SyncLogRepository extends JpaRepository<SyncLog, Long> {

    List<SyncLog> findByTransporteurOrderByDateCreationDesc(Utilisateur transporteur);

    @Modifying
    @Query("DELETE FROM SyncLog s WHERE s.dateCreation < :dateLimite")
    int supprimerLogsAnciens(@Param("dateLimite") LocalDateTime dateLimite);
}
