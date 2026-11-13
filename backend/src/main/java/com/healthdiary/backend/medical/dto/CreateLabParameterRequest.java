package com.healthdiary.backend.medical.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateLabParameterRequest(
        @NotNull Long serviceId,
        @NotBlank @Size(max = 200) String name,
        @Size(max = 50) String groupKey
) {
}
