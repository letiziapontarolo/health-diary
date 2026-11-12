package com.healthdiary.backend.medical.dto;

import com.healthdiary.backend.medical.entity.MedicalEventStatus;
import com.healthdiary.backend.medical.entity.MedicalServiceCategory;

import java.time.LocalDate;

public record MedicalEventFilter(
        MedicalServiceCategory category,
        MedicalEventStatus status,
        Long doctorId,
        Long facilityId,
        LocalDate fromDate,
        LocalDate toDate
) {
}