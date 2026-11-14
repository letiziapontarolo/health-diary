package com.healthdiary.backend.medical.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record SetGeneralPractitionerRequest(@NotNull Long doctorId, LocalDate startDate) {
}
