package com.healthdiary.backend.medical.service;

import com.healthdiary.backend.medical.entity.MedicalEventStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MedicalEventStatusCalculatorTest {

    private MedicalEventStatusCalculator calculator;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(
                Instant.parse("2026-11-12T13:00:00Z"),
                ZoneId.of("Europe/Rome")
        );

        calculator = new MedicalEventStatusCalculator(clock);
    }

    @Test
    void shouldScheduleFutureEvent() {
        assertEquals(
                MedicalEventStatus.SCHEDULED,
                calculator.calculate(
                        LocalDate.of(2026, 11, 20),
                        LocalTime.of(10, 30)
                )
        );
    }

    @Test
    void shouldCompletePastEvent() {
        assertEquals(
                MedicalEventStatus.COMPLETED,
                calculator.calculate(
                        LocalDate.of(2026, 11, 10),
                        LocalTime.of(10, 30)
                )
        );
    }

    @Test
    void shouldScheduleTodayEventWithoutTime() {
        assertEquals(
                MedicalEventStatus.SCHEDULED,
                calculator.calculate(
                        LocalDate.of(2026, 11, 12),
                        null
                )
        );
    }

    @Test
    void shouldScheduleTodayEventWithFutureTime() {
        assertEquals(
                MedicalEventStatus.SCHEDULED,
                calculator.calculate(
                        LocalDate.of(2026, 11, 12),
                        LocalTime.of(16, 0)
                )
        );
    }

    @Test
    void shouldCompleteTodayEventWithPastTime() {
        assertEquals(
                MedicalEventStatus.COMPLETED,
                calculator.calculate(
                        LocalDate.of(2026, 11, 12),
                        LocalTime.of(12, 0)
                )
        );
    }

    @Test
    void shouldCompleteTodayEventAtExactTime() {
        assertEquals(
                MedicalEventStatus.COMPLETED,
                calculator.calculate(
                        LocalDate.of(2026, 11, 12),
                        LocalTime.of(14, 0)
                )
        );
    }
}
