
package com.healthdiary.backend.medical.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record UpdateMedicalEventRequest(

        @NotNull
        Long serviceId,

        @NotNull
        LocalDate eventDate,

        LocalTime eventTime,

        Long doctorId,

        Long facilityId,

        String notes,

        List<Long> optionIds

) {
}
