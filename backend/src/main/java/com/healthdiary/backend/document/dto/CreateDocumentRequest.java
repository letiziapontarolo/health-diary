package com.healthdiary.backend.document.dto;

import java.time.LocalDate;

public record CreateDocumentRequest(
        String displayName,
        Long categoryId,
        LocalDate documentDate,
        LocalDate expiryDate,
        String notes,
        boolean report
) {
}
