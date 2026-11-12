
package com.healthdiary.backend.medical.service;

import com.healthdiary.backend.medical.entity.MedicalEventStatus;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Component
public class MedicalEventStatusCalculator {

    private final Clock clock;

    public MedicalEventStatusCalculator(Clock clock) {
        this.clock = clock;
    }

    public MedicalEventStatus calculate(
            LocalDate eventDate,
            LocalTime eventTime
    ) {
        LocalDateTime now = LocalDateTime.now(clock);

        if (eventDate.isBefore(now.toLocalDate())) {
            return MedicalEventStatus.COMPLETED;
        }

        if (eventDate.isAfter(now.toLocalDate())) {
            return MedicalEventStatus.SCHEDULED;
        }

        if (eventTime == null) {
            return MedicalEventStatus.SCHEDULED;
        }

        LocalDateTime eventDateTime =
                LocalDateTime.of(eventDate, eventTime);

        return eventDateTime.isAfter(now)
                ? MedicalEventStatus.SCHEDULED
                : MedicalEventStatus.COMPLETED;
    }
}
