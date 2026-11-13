package com.healthdiary.backend.medical.dto;

public record LabParameterResponse(
        Long id,
        Long serviceId,
        String name,
        String groupKey,
        boolean active,
        boolean custom
) {
}
