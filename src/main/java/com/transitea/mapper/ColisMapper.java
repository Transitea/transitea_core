package com.transitea.mapper;

import com.transitea.dto.response.ColisReponse;
import com.transitea.dto.response.MiseAJourStatutReponse;
import com.transitea.entity.Colis;
import com.transitea.entity.MiseAJourStatut;
import com.transitea.entity.Utilisateur;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ColisMapper {

    @Mapping(source = "transporteur.id", target = "transporteurId")
    @Mapping(source = "transporteur", target = "transporteurNomComplet", qualifiedByName = "versNomComplet")
    @Mapping(target = "historique", ignore = true)
    ColisReponse versReponse(Colis colis);

    @Mapping(source = "transporteur.id", target = "transporteurId")
    @Mapping(source = "transporteur", target = "transporteurNomComplet", qualifiedByName = "versNomComplet")
    @Mapping(target = "historique", ignore = true)
    List<ColisReponse> versReponses(List<Colis> colis);

    @Mapping(source = "utilisateur.id", target = "utilisateurId")
    MiseAJourStatutReponse versReponse(MiseAJourStatut miseAJourStatut);

    List<MiseAJourStatutReponse> versMiseAJourReponses(List<MiseAJourStatut> miseAJours);

    @Named("versNomComplet")
    default String versNomComplet(Utilisateur utilisateur) {
        if (utilisateur == null) {
            return null;
        }
        return utilisateur.getPrenom() + " " + utilisateur.getNom();
    }
}
