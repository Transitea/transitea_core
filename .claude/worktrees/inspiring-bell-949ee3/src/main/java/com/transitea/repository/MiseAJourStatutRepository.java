package com.transitea.repository;

import com.transitea.entity.Colis;
import com.transitea.entity.MiseAJourStatut;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MiseAJourStatutRepository extends JpaRepository<MiseAJourStatut, Long> {

    List<MiseAJourStatut> findByColisOrderByDateCreationAsc(Colis colis);

    List<MiseAJourStatut> findByColisOrderByDateCreationDesc(Colis colis);
}
