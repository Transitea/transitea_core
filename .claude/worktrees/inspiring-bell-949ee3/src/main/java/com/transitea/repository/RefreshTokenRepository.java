package com.transitea.repository;

import com.transitea.entity.RefreshToken;
import com.transitea.entity.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    @Modifying
    @Query("UPDATE RefreshToken r SET r.revoque = true WHERE r.utilisateur = :utilisateur AND r.revoque = false")
    int revoquerTousLesTokensUtilisateur(@Param("utilisateur") Utilisateur utilisateur);

    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.dateExpiration < :dateLimite OR r.revoque = true")
    int supprimerTokensExpires(@Param("dateLimite") LocalDateTime dateLimite);
}
