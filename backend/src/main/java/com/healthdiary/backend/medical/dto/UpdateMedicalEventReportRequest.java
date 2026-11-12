package com.healthdiary.backend.medical.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateMedicalEventReportRequest(
        @NotBlank String reportText
) {
}
