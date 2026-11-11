package com.healthdiary.backend.medical.dto;

import com.healthdiary.backend.medical.entity.MedicalServiceCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateMedicalServiceRequest(
        @NotBlank
        @Size(max = 255)
        String name,

        @NotNull
        MedicalServiceCategory category
) {
}
