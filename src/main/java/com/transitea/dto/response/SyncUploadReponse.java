package com.transitea.dto.response;

import java.util.List;

public record SyncUploadReponse(
        int nbColisEnvoyes,
        int nbColisReussis,
        int nbColisDoublons,
        int nbColisEchecs,
        List<ResultatSyncColisReponse> colisResultats,
        int nbStatutsEnvoyes,
        int nbStatutsReussis,
        int nbStatutsConflits,
        int nbStatutsEchecs,
        List<ResultatSyncStatutReponse> statutResultats
) {
}
