package com.transitea.repository;

import com.transitea.entity.Agence;
import com.transitea.entity.Enseigne;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AgenceRepository extends JpaRepository<Agence, Long> {

    Optional<Agence> findByIdAndSupprimeFalse(Long id);

    List<Agence> findByEnseigneAndSupprimeFalse(Enseigne enseigne);

    List<Agence> findBySupprimeFalse();
}
