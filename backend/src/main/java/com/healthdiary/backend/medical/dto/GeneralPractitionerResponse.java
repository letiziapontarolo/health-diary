package com.healthdiary.backend.medical.dto;

import java.time.LocalDate;

public record GeneralPractitionerResponse(Long id, Long doctorId, String doctorName,
        LocalDate startDate, LocalDate endDate, boolean current) {
}
