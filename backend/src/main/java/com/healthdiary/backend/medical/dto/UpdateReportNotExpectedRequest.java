package com.healthdiary.backend.medical.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateReportNotExpectedRequest(
        @NotNull Boolean reportNotExpected
) {
}
