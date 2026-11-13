package com.healthdiary.backend.document.dto;

import java.time.LocalDate;

public record DocumentFilter(
        String name,
        Long categoryId,
        LocalDate fromDate,
        LocalDate toDate,
        String notes,
        Boolean expiredOnly
) {
}
