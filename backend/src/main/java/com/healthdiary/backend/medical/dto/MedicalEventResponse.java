package com.healthdiary.backend.medical.dto;

import com.healthdiary.backend.medical.entity.MedicalEventStatus;
import com.healthdiary.backend.medical.entity.MedicalEventStatusMode;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record MedicalEventResponse(

        Long id,

        Long serviceId,

        String serviceName,

        String category,

        LocalDate eventDate,

        LocalTime eventTime,

        MedicalEventStatus status,

        MedicalEventStatusMode statusMode,

        Long doctorId,

        String doctorName,

        Long facilityId,

        String facilityName,

        String notes,

        String reportText,

        boolean reportNotExpected,

        boolean reportMissing,

        List<Long> optionIds

) {
}
