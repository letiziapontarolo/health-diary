package com.healthdiary.backend.document.dto;

public record DocumentCategoryResponse(
        Long id,
        String name,
        boolean isDefault
) {
}
