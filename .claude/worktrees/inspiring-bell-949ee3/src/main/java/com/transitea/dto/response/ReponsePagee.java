package com.transitea.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;

public record ReponsePagee<T>(
        List<T> contenu,
        int pageCourante,
        int totalPages,
        long totalElements,
        int taillePage,
        boolean dernierePage
) {
    public static <T> ReponsePagee<T> depuis(Page<T> page) {
        return new ReponsePagee<>(
                page.getContent(),
                page.getNumber(),
                page.getTotalPages(),
                page.getTotalElements(),
                page.getSize(),
                page.isLast()
        );
    }
}
