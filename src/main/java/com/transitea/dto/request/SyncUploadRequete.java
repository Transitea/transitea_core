package com.transitea.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.util.List;

public record SyncUploadRequete(

        @Size(max = 100, message = "Maximum 100 colis par synchronisation")
        @Valid
        List<CreationColisRequete> colis,

        @Size(max = 100, message = "Maximum 100 mises a jour de statut par synchronisation")
        @Valid
        List<MiseAJourStatutSyncItem> misesAJourStatut
) {
    public SyncUploadRequete {
        if (colis == null) {
            colis = List.of();
        }
        if (misesAJourStatut == null) {
            misesAJourStatut = List.of();
        }
    }
}
