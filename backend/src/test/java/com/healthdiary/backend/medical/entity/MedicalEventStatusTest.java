package com.healthdiary.backend.medical.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MedicalEventStatusTest {

    private MedicalEvent createEvent() {
        return new MedicalEvent(
                null,
                null,
                LocalDate.of(2026, 11, 10),
                LocalTime.of(10, 0),
                null,
                null,
                null);
    }

    @Test
    void shouldChangeCompletedEventToScheduled() {
        MedicalEvent event = createEvent();

        event.updateAutomaticStatus(MedicalEventStatus.COMPLETED);

        event.updateAutomaticStatus(MedicalEventStatus.SCHEDULED);

        assertEquals(MedicalEventStatus.SCHEDULED, event.getStatus());
    }

    @Test
    void shouldKeepCancelledEventCancelled() {
        MedicalEvent event = createEvent();

        event.cancel();

        event.updateAutomaticStatus(MedicalEventStatus.COMPLETED);

        assertEquals(MedicalEventStatus.CANCELLED, event.getStatus());
        assertEquals(MedicalEventStatusMode.MANUAL, event.getStatusMode());
    }

    @Test
    void shouldSaveReportTextAndTrimWhitespace() {
        MedicalEvent event = createEvent();

        event.updateReportText("  Esame nella norma  ");

        assertEquals("Esame nella norma", event.getReportText());
        assertEquals(false, event.isReportNotExpected());
    }

    @Test
    void shouldReplaceExistingReportText() {
        MedicalEvent event = createEvent();

        event.updateReportText("Primo referto");
        event.updateReportText("Referto aggiornato");

        assertEquals("Referto aggiornato", event.getReportText());
    }

    @Test
    void shouldRejectBlankReportText() {
        MedicalEvent event = createEvent();

        assertThrows(
                IllegalArgumentException.class,
                () -> event.updateReportText("   "));

        assertThrows(
                IllegalArgumentException.class,
                () -> event.updateReportText(null));
    }

    @Test
    void shouldDeleteReportText() {
        MedicalEvent event = createEvent();

        event.updateReportText("Referto esistente");
        event.deleteReportText();

        assertNull(event.getReportText());
        assertFalse(event.isReportNotExpected());
    }

    @Test
    void shouldClearReportWhenNotExpected() {
        MedicalEvent event = createEvent();

        event.updateReportText("Referto esistente");
        event.setReportNotExpected(true);

        assertNull(event.getReportText());
        assertTrue(event.isReportNotExpected());

        event.setReportNotExpected(false);

        assertFalse(event.isReportNotExpected());
        assertNull(event.getReportText());
    }

    @Test
    void shouldReportMissingForCompletedEventWithoutReport() {
        MedicalEvent event = createEvent();
        event.updateAutomaticStatus(MedicalEventStatus.COMPLETED);

        assertTrue(event.isReportMissing());
    }

    @Test
    void shouldNotReportMissingForScheduledEvent() {
        MedicalEvent event = createEvent();

        assertFalse(event.isReportMissing());
    }

    @Test
    void shouldNotReportMissingWhenReportExists() {
        MedicalEvent event = createEvent();
        event.updateAutomaticStatus(MedicalEventStatus.COMPLETED);
        event.updateReportText("Esame nella norma");

        assertFalse(event.isReportMissing());
    }

    @Test
    void shouldNotReportMissingWhenReportNotExpected() {
        MedicalEvent event = createEvent();
        event.updateAutomaticStatus(MedicalEventStatus.COMPLETED);
        event.setReportNotExpected(true);

        assertFalse(event.isReportMissing());
    }

    @Test
    void shouldNotReportMissingForCancelledEvent() {
        MedicalEvent event = createEvent();
        event.cancel();

        assertFalse(event.isReportMissing());
    }

}
