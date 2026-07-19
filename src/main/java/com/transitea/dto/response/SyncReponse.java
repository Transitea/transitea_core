package com.transitea.dto.response;

import java.util.List;

public record SyncReponse(
        int nbEnvoyes,
        int nbReussis,
        int nbDoublons,
        int nbEchecs,
        List<ResultatSyncColisReponse> resultats
) {
}
