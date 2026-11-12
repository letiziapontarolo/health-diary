package com.healthdiary.backend.medical.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateDoctorRequest(
        @NotBlank
        @Size(max = 200)
        String name,

        @Size(max = 150)
        String specialization
) {
}
