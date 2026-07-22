package com.transitea.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record SyncDownloadReponse(
        List<ColisReponse> colis,
        LocalDateTime curseur
) {
}
