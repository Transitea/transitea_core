package com.transitea.repository;

import com.transitea.entity.Agence;
import com.transitea.entity.Utilisateur;
import com.transitea.entity.enums.Role;
import com.transitea.entity.enums.StatutUtilisateur;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {

    Optional<Utilisateur> findByEmailAndSupprimeFalse(String email);

    Optional<Utilisateur> findByUuidAndSupprimeFalse(String uuid);

    Optional<Utilisateur> findByIdAndSupprimeFalse(Long id);

    boolean existsByEmail(String email);

    boolean existsByTelephone(String telephone);

    List<Utilisateur> findByStatutAndSupprimeFalse(StatutUtilisateur statut);

    List<Utilisateur> findByAgenceAndRoleAndSupprimeFalse(Agence agence, Role role);

    Page<Utilisateur> findBySupprimeFalse(Pageable pageable);

    Page<Utilisateur> findByAgenceAndSupprimeFalse(Agence agence, Pageable pageable);
}
