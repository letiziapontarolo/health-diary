package com.healthdiary.backend.medical.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateMedicalServiceRequest(
        @NotBlank
        @Size(max = 255)
        String name
) {
}