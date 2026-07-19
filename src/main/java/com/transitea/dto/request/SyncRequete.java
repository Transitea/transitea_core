package com.transitea.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record SyncRequete(

        @NotEmpty(message = "La liste des colis ne peut pas etre vide")
        @Size(max = 100, message = "Maximum 100 colis par synchronisation")
        @Valid
        List<CreationColisRequete> colis
) {
}
