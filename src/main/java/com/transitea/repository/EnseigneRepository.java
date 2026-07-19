package com.transitea.repository;

import com.transitea.entity.Enseigne;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EnseigneRepository extends JpaRepository<Enseigne, Long> {
}
