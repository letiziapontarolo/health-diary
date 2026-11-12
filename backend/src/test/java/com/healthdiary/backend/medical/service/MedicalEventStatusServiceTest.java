
package com.healthdiary.backend.medical.service;

import com.healthdiary.backend.medical.entity.*;
import com.healthdiary.backend.medical.repository.MedicalEventRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MedicalEventStatusServiceTest {

    @Mock
    private MedicalEventRepository eventRepository;

    private MedicalEventStatusService statusService;

    private final Clock clock = Clock.fixed(
            LocalDateTime.of(2026, 11, 12, 14, 0)
                    .atZone(ZoneId.of("Europe/Rome"))
                    .toInstant(),
            ZoneId.of("Europe/Rome"));

    @BeforeEach
    void setUp() {
        statusService = new MedicalEventStatusService(
                eventRepository,
                new MedicalEventStatusCalculator(clock));
    }

    private MedicalEvent event(
            LocalDate date,
            LocalTime time) {
        return new MedicalEvent(
                null,
                null,
                date,
                time,
                null,
                null,
                null);
    }

    @Test
    void shouldCompletePastEventWithoutTime() {
        MedicalEvent event = event(
                LocalDate.of(2026, 11, 11),
                null);

        when(eventRepository.findByStatusAndStatusMode(
                MedicalEventStatus.SCHEDULED,
                MedicalEventStatusMode.AUTO)).thenReturn(List.of(event));

        int updated = statusService.updateAutomaticStatuses();

        assertEquals(1, updated);
        assertEquals(MedicalEventStatus.COMPLETED,
                event.getStatus());
    }

    @Test
    void shouldCompleteEventWhenTimeIsReached() {
        MedicalEvent event = event(
                LocalDate.of(2026, 11, 12),
                LocalTime.of(14, 0));

        when(eventRepository.findByStatusAndStatusMode(
                MedicalEventStatus.SCHEDULED,
                MedicalEventStatusMode.AUTO)).thenReturn(List.of(event));

        int updated = statusService.updateAutomaticStatuses();

        assertEquals(1, updated);
        assertEquals(MedicalEventStatus.COMPLETED,
                event.getStatus());
    }

    @Test
    void shouldNotCompleteFutureEvent() {
        MedicalEvent event = event(
                LocalDate.of(2026, 11, 13),
                null);

        when(eventRepository.findByStatusAndStatusMode(
                MedicalEventStatus.SCHEDULED,
                MedicalEventStatusMode.AUTO)).thenReturn(List.of(event));

        int updated = statusService.updateAutomaticStatuses();

        assertEquals(0, updated);
        assertEquals(MedicalEventStatus.SCHEDULED,
                event.getStatus());
    }

    @Test
    void shouldNotCompleteTodayEventWithoutTime() {
        MedicalEvent event = event(
                LocalDate.of(2026, 11, 12),
                null);

        when(eventRepository.findByStatusAndStatusMode(
                MedicalEventStatus.SCHEDULED,
                MedicalEventStatusMode.AUTO)).thenReturn(List.of(event));

        int updated = statusService.updateAutomaticStatuses();

        assertEquals(0, updated);
        assertEquals(MedicalEventStatus.SCHEDULED,
                event.getStatus());
    }

    @Test
    void shouldNotCompleteEventBeforeScheduledTime() {
        MedicalEvent event = event(
                LocalDate.of(2026, 11, 12),
                LocalTime.of(15, 0));

        when(eventRepository.findByStatusAndStatusMode(
                MedicalEventStatus.SCHEDULED,
                MedicalEventStatusMode.AUTO)).thenReturn(List.of(event));

        int updated = statusService.updateAutomaticStatuses();

        assertEquals(0, updated);
        assertEquals(MedicalEventStatus.SCHEDULED,
                event.getStatus());
    }

    @Test
    void shouldUpdateMultiplePastEvents() {
        MedicalEvent first = event(
                LocalDate.of(2026, 11, 10),
                null);

        MedicalEvent second = event(
                LocalDate.of(2026, 11, 12),
                LocalTime.of(13, 30));

        MedicalEvent future = event(
                LocalDate.of(2026, 11, 13),
                null);

        when(eventRepository.findByStatusAndStatusMode(
                MedicalEventStatus.SCHEDULED,
                MedicalEventStatusMode.AUTO)).thenReturn(List.of(first, second, future));

        int updated = statusService.updateAutomaticStatuses();

        assertEquals(2, updated);
        assertEquals(MedicalEventStatus.COMPLETED,
                first.getStatus());
        assertEquals(MedicalEventStatus.COMPLETED,
                second.getStatus());
        assertEquals(MedicalEventStatus.SCHEDULED,
                future.getStatus());
    }

    @Test
    void shouldNotModifyCancelledEvent() {
        MedicalEvent event = event(
                LocalDate.of(2026, 11, 10),
                null);

        event.cancel();

        when(eventRepository.findByStatusAndStatusMode(
                MedicalEventStatus.SCHEDULED,
                MedicalEventStatusMode.AUTO)).thenReturn(List.of());

        int updated = statusService.updateAutomaticStatuses();

        assertEquals(0, updated);
        assertEquals(MedicalEventStatus.CANCELLED,
                event.getStatus());
    }
}
