package com.transitea.mapper;

import com.transitea.dto.response.ColisReponse;
import com.transitea.dto.response.MiseAJourStatutReponse;
import com.transitea.entity.Agence;
import com.transitea.entity.Colis;
import com.transitea.entity.MiseAJourStatut;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ColisMapper {

    @Mapping(source = "agenceOrigine.id", target = "agenceOrigineId")
    @Mapping(source = "agenceOrigine", target = "agenceOrigineNom", qualifiedByName = "versNomAgence")
    @Mapping(source = "agenceRetrait.id", target = "agenceRetraitId")
    @Mapping(source = "agenceRetrait", target = "agenceRetraitNom", qualifiedByName = "versNomAgence")
    @Mapping(target = "historique", ignore = true)
    ColisReponse versReponse(Colis colis);

    @Mapping(source = "agenceOrigine.id", target = "agenceOrigineId")
    @Mapping(source = "agenceOrigine", target = "agenceOrigineNom", qualifiedByName = "versNomAgence")
    @Mapping(source = "agenceRetrait.id", target = "agenceRetraitId")
    @Mapping(source = "agenceRetrait", target = "agenceRetraitNom", qualifiedByName = "versNomAgence")
    @Mapping(target = "historique", ignore = true)
    List<ColisReponse> versReponses(List<Colis> colis);

    @Mapping(source = "utilisateur.id", target = "utilisateurId")
    MiseAJourStatutReponse versReponse(MiseAJourStatut miseAJourStatut);

    List<MiseAJourStatutReponse> versMiseAJourReponses(List<MiseAJourStatut> miseAJours);

    @Named("versNomAgence")
    default String versNomAgence(Agence agence) {
        return agence == null ? null : agence.getNom();
    }
}
