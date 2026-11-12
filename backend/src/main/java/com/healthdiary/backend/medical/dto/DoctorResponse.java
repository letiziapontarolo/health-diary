package com.healthdiary.backend.medical.dto;

public record DoctorResponse(
        Long id,
        String name,
        String specialization
) {
}
