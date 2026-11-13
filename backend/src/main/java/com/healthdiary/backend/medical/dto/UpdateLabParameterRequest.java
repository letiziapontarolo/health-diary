package com.healthdiary.backend.medical.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateLabParameterRequest(
        @NotBlank @Size(max = 200) String name
) {
}
