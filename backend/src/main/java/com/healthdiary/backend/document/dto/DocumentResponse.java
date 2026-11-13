package com.healthdiary.backend.document.dto;

import java.time.Instant;
import java.time.LocalDate;

public record DocumentResponse(
        Long id,
        String originalName,
        String displayName,
        String contentType,
        long fileSize,
        Long categoryId,
        String category,
        LocalDate documentDate,
        LocalDate expiryDate,
        String notes,
        boolean report,
        boolean expired,
        Instant createdAt,
        Instant updatedAt
) {
}
