package com.transitea.dto.response;

import java.time.LocalDate;

public record VolumeJourReponse(
        LocalDate date,
        long total
) {
}
