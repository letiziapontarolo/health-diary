package com.healthdiary.backend.medical.dto;

public record MeasurementUnitResponse(
        Long id,
        String symbol,
        String name
) {
}
