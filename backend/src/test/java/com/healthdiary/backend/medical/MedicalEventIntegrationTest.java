
package com.healthdiary.backend.medical;

import com.healthdiary.backend.auth.entity.User;
import com.healthdiary.backend.auth.repository.UserRepository;
import com.healthdiary.backend.medical.dto.CreateMedicalEventRequest;
import com.healthdiary.backend.medical.dto.MedicalEventResponse;
import com.healthdiary.backend.medical.dto.UpdateMedicalEventRequest;
import com.healthdiary.backend.medical.entity.*;
import com.healthdiary.backend.medical.repository.*;
import com.healthdiary.backend.medical.service.MedicalEventService;
import com.healthdiary.backend.medical.dto.MedicalEventFilter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.Clock;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Transactional
class MedicalEventIntegrationTest {

        @Container
        @ServiceConnection
        static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private MedicalServiceRepository serviceRepository;

        @Autowired
        private MedicalEventRepository eventRepository;

        @Autowired
        private Clock clock;

        @Autowired
        private DoctorRepository doctorRepository;

        @Autowired
        private FacilityRepository facilityRepository;

        @Autowired
        private MedicalServiceOptionRepository optionRepository;

        @Autowired
        private MedicalEventOptionRepository eventOptionRepository;

        @Autowired
        private MedicalEventService medicalEventService;

        private User mario;
        private User giulia;
        private MedicalService service;

        @BeforeEach
        void setUp() {
                mario = userRepository.save(new User(
                                "Mario",
                                "Rossi",
                                "mario.integration@test.com",
                                "test-password-hash",
                                LocalDate.of(1995, 5, 10)));

                giulia = userRepository.save(new User(
                                "Giulia",
                                "Bianchi",
                                "giulia.integration@test.com",
                                "test-password-hash",
                                LocalDate.of(1997, 8, 15)));

                service = serviceRepository.save(new MedicalService(
                                null,
                                "Visita di controllo integration",
                                MedicalServiceCategory.SPECIALIST,
                                null));
        }

        private String validRequest() {
                return """
                                {
                                    "serviceId": %d,
                                    "eventDate": "2026-11-20",
                                    "eventTime": "10:30",
                                    "notes": "Controllo periodico"
                                }
                                """.formatted(service.getId());
        }

        private MedicalEvent createReportTestEvent() {
                return eventRepository.save(new MedicalEvent(
                                mario,
                                service,
                                LocalDate.now(clock).minusDays(1),
                                null,
                                null,
                                null,
                                "Prestazione con referto"));
        }

        @Test
        void shouldCreateAndPersistMedicalEvent() throws Exception {
                mockMvc.perform(post("/api/medical-events")
                                .with(user(mario.getEmail()))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(validRequest()))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.serviceName")
                                                .value("Visita di controllo integration"))
                                .andExpect(jsonPath("$.status")
                                                .value("SCHEDULED"))
                                .andExpect(jsonPath("$.statusMode")
                                                .value("AUTO"));

                var events = eventRepository
                                .findByUserIdOrderByEventDateDescEventTimeDesc(
                                                mario.getId());

                assertEquals(1, events.size());
                assertEquals(LocalDate.of(2026, 11, 20),
                                events.get(0).getEventDate());
                assertEquals(mario.getId(),
                                events.get(0).getUser().getId());
        }

        @Test
        void shouldReturnOnlyAuthenticatedUsersEvents() throws Exception {
                eventRepository.save(new MedicalEvent(
                                mario,
                                service,
                                LocalDate.of(2026, 11, 20),
                                null,
                                null,
                                null,
                                "Evento di Mario"));

                eventRepository.save(new MedicalEvent(
                                giulia,
                                service,
                                LocalDate.of(2026, 11, 21),
                                null,
                                null,
                                null,
                                "Evento di Giulia"));

                mockMvc.perform(get("/api/medical-events")
                                .with(user(mario.getEmail())))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(1))
                                .andExpect(jsonPath("$[0].notes")
                                                .value("Evento di Mario"));
        }

        @Test
        void shouldPreventAccessToAnotherUsersEvent() throws Exception {
                MedicalEvent event = eventRepository.save(
                                new MedicalEvent(
                                                mario,
                                                service,
                                                LocalDate.of(2026, 11, 20),
                                                null,
                                                null,
                                                null,
                                                "Evento privato"));

                mockMvc.perform(get("/api/medical-events/" + event.getId())
                                .with(user(giulia.getEmail())))
                                .andExpect(status().isNotFound());
        }

        @Test
        void shouldRejectArchivedService() throws Exception {
                service.archive();
                serviceRepository.save(service);

                mockMvc.perform(post("/api/medical-events")
                                .with(user(mario.getEmail()))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(validRequest()))
                                .andExpect(status().isBadRequest());

                assertTrue(eventRepository
                                .findByUserIdOrderByEventDateDescEventTimeDesc(
                                                mario.getId())
                                .isEmpty());
        }

        @Test
        void shouldRejectInvalidRequestWithoutPersisting() throws Exception {
                mockMvc.perform(post("/api/medical-events")
                                .with(user(mario.getEmail()))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                    "eventDate": "2026-11-20"
                                                }
                                                """))
                                .andExpect(status().isBadRequest());

                assertTrue(eventRepository
                                .findByUserIdOrderByEventDateDescEventTimeDesc(
                                                mario.getId())
                                .isEmpty());
        }

        @Test
        void shouldRejectUnauthenticatedRequests() throws Exception {
                mockMvc.perform(get("/api/medical-events"))
                                .andExpect(status().isUnauthorized());

                mockMvc.perform(post("/api/medical-events")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(validRequest()))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        void shouldUpdateExistingMedicalEvent() throws Exception {
                MedicalEvent event = eventRepository.save(new MedicalEvent(
                                mario,
                                service,
                                LocalDate.of(2026, 11, 20),
                                null,
                                null,
                                null,
                                "Note originali"));

                String request = """
                                {
                                    "serviceId": %d,
                                    "eventDate": "2026-12-10",
                                    "eventTime": "15:45",
                                    "notes": "Note aggiornate"
                                }
                                """.formatted(service.getId());

                mockMvc.perform(put("/api/medical-events/" + event.getId())
                                .with(user(mario.getEmail()))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.eventDate")
                                                .value("2026-12-10"))
                                .andExpect(jsonPath("$.eventTime")
                                                .value("15:45:00"))
                                .andExpect(jsonPath("$.notes")
                                                .value("Note aggiornate"));

                eventRepository.flush();

                MedicalEvent updated = eventRepository
                                .findByIdAndUserId(event.getId(), mario.getId())
                                .orElseThrow();

                assertEquals(LocalDate.of(2026, 12, 10),
                                updated.getEventDate());
                assertEquals("Note aggiornate", updated.getNotes());
        }

        @Test
        void shouldPreventUpdatingAnotherUsersEvent() throws Exception {
                MedicalEvent event = eventRepository.save(new MedicalEvent(
                                mario,
                                service,
                                LocalDate.of(2026, 11, 20),
                                null,
                                null,
                                null,
                                "Evento privato"));

                mockMvc.perform(put("/api/medical-events/" + event.getId())
                                .with(user(giulia.getEmail()))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(validRequest()))
                                .andExpect(status().isNotFound());
        }

        @Test
        void shouldRejectUpdatingNonexistentEvent() throws Exception {
                mockMvc.perform(put("/api/medical-events/999999")
                                .with(user(mario.getEmail()))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(validRequest()))
                                .andExpect(status().isNotFound());
        }

        @Test
        void shouldRejectUpdatingWithArchivedService() throws Exception {
                MedicalEvent event = eventRepository.save(new MedicalEvent(
                                mario,
                                service,
                                LocalDate.of(2026, 11, 20),
                                null,
                                null,
                                null,
                                "Note originali"));

                service.archive();
                serviceRepository.save(service);

                mockMvc.perform(put("/api/medical-events/" + event.getId())
                                .with(user(mario.getEmail()))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(validRequest()))
                                .andExpect(status().isBadRequest());

                assertEquals("Note originali", event.getNotes());
        }

        @Test
        void shouldRejectInvalidUpdateRequest() throws Exception {
                MedicalEvent event = eventRepository.save(new MedicalEvent(
                                mario,
                                service,
                                LocalDate.of(2026, 11, 20),
                                null,
                                null,
                                null,
                                "Note originali"));

                mockMvc.perform(put("/api/medical-events/" + event.getId())
                                .with(user(mario.getEmail()))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                    "eventDate": "2026-12-10"
                                                }
                                                """))
                                .andExpect(status().isBadRequest());

                assertEquals("Note originali", event.getNotes());
        }

        @Test
        void shouldCancelMedicalEvent() throws Exception {
                MedicalEvent event = eventRepository.save(new MedicalEvent(
                                mario, service, LocalDate.of(2026, 12, 20),
                                null, null, null, "Evento da annullare"));

                mockMvc.perform(patch("/api/medical-events/" + event.getId() + "/cancel")
                                .with(user(mario.getEmail()))
                                .with(csrf()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("CANCELLED"))
                                .andExpect(jsonPath("$.statusMode").value("MANUAL"));

                eventRepository.flush();

                assertEquals(MedicalEventStatus.CANCELLED, event.getStatus());
                assertTrue(eventRepository.findById(event.getId()).isPresent());
        }

        @Test
        void shouldRejectCancellingAlreadyCancelledEvent() throws Exception {
                MedicalEvent event = eventRepository.save(new MedicalEvent(
                                mario, service, LocalDate.of(2026, 12, 20),
                                null, null, null, null));
                event.cancel();

                mockMvc.perform(patch("/api/medical-events/" + event.getId() + "/cancel")
                                .with(user(mario.getEmail()))
                                .with(csrf()))
                                .andExpect(status().isConflict());
        }

        @Test
        void shouldRestoreFutureEventAsScheduled() throws Exception {
                MedicalEvent event = eventRepository.save(new MedicalEvent(
                                mario, service, LocalDate.now().plusDays(10),
                                null, null, null, null));
                event.cancel();

                mockMvc.perform(patch("/api/medical-events/" + event.getId() + "/restore")
                                .with(user(mario.getEmail()))
                                .with(csrf()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("SCHEDULED"))
                                .andExpect(jsonPath("$.statusMode").value("AUTO"));
        }

        @Test
        void shouldRestorePastEventAsCompleted() throws Exception {
                MedicalEvent event = eventRepository.save(new MedicalEvent(
                                mario, service, LocalDate.now().minusDays(10),
                                null, null, null, null));
                event.cancel();

                mockMvc.perform(patch("/api/medical-events/" + event.getId() + "/restore")
                                .with(user(mario.getEmail()))
                                .with(csrf()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("COMPLETED"))
                                .andExpect(jsonPath("$.statusMode").value("AUTO"));
        }

        @Test
        void shouldRejectRestoringNonCancelledEvent() throws Exception {
                MedicalEvent event = eventRepository.save(new MedicalEvent(
                                mario, service, LocalDate.now().plusDays(10),
                                null, null, null, null));

                mockMvc.perform(patch("/api/medical-events/" + event.getId() + "/restore")
                                .with(user(mario.getEmail()))
                                .with(csrf()))
                                .andExpect(status().isConflict());
        }

        @Test
        void shouldPreventCancellingAnotherUsersEvent() throws Exception {
                MedicalEvent event = eventRepository.save(new MedicalEvent(
                                mario, service, LocalDate.now().plusDays(10),
                                null, null, null, null));

                mockMvc.perform(patch("/api/medical-events/" + event.getId() + "/cancel")
                                .with(user(giulia.getEmail()))
                                .with(csrf()))
                                .andExpect(status().isNotFound());
        }

        @Test
        void shouldPreventRestoringAnotherUsersEvent() throws Exception {
                MedicalEvent event = eventRepository.save(new MedicalEvent(
                                mario, service, LocalDate.now().plusDays(10),
                                null, null, null, null));
                event.cancel();

                mockMvc.perform(patch("/api/medical-events/" + event.getId() + "/restore")
                                .with(user(giulia.getEmail()))
                                .with(csrf()))
                                .andExpect(status().isNotFound());
        }

        @Test
        void shouldRescheduleCompletedEventWhenMovedToFuture() throws Exception {
                LocalDate pastDate = LocalDate.now(clock).minusDays(10);
                LocalDate futureDate = LocalDate.now(clock).plusDays(10);

                MedicalEvent event = new MedicalEvent(
                                mario, service, pastDate,
                                null, null, null, "Prestazione completata");

                event.updateAutomaticStatus(MedicalEventStatus.COMPLETED);
                event = eventRepository.save(event);

                String request = """
                                {
                                    "serviceId": %d,
                                    "eventDate": "%s",
                                    "notes": "Prestazione riprogrammata"
                                }
                                """.formatted(service.getId(), futureDate);

                mockMvc.perform(put("/api/medical-events/" + event.getId())
                                .with(user(mario.getEmail()))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("SCHEDULED"))
                                .andExpect(jsonPath("$.statusMode").value("AUTO"));

                eventRepository.flush();

                assertEquals(MedicalEventStatus.SCHEDULED, event.getStatus());
                assertEquals(futureDate, event.getEventDate());
        }

        @Test
        void shouldCompleteScheduledEventWhenMovedToPast() throws Exception {
                LocalDate futureDate = LocalDate.now(clock).plusDays(10);
                LocalDate pastDate = LocalDate.now(clock).minusDays(10);

                MedicalEvent event = eventRepository.save(new MedicalEvent(
                                mario, service, futureDate,
                                null, null, null, "Prestazione programmata"));

                String request = """
                                {
                                    "serviceId": %d,
                                    "eventDate": "%s",
                                    "notes": "Data modificata"
                                }
                                """.formatted(service.getId(), pastDate);

                mockMvc.perform(put("/api/medical-events/" + event.getId())
                                .with(user(mario.getEmail()))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("COMPLETED"))
                                .andExpect(jsonPath("$.statusMode").value("AUTO"));

                eventRepository.flush();

                assertEquals(MedicalEventStatus.COMPLETED, event.getStatus());
                assertEquals(pastDate, event.getEventDate());
        }

        @Test
        void shouldKeepCancelledEventCancelledWhenDateChanges() throws Exception {
                LocalDate futureDate = LocalDate.now(clock).plusDays(10);
                LocalDate newDate = LocalDate.now(clock).plusDays(20);

                MedicalEvent event = new MedicalEvent(
                                mario, service, futureDate,
                                null, null, null, "Prestazione annullata");

                event.cancel();
                event = eventRepository.save(event);

                String request = """
                                {
                                    "serviceId": %d,
                                    "eventDate": "%s",
                                    "notes": "Data modificata"
                                }
                                """.formatted(service.getId(), newDate);

                mockMvc.perform(put("/api/medical-events/" + event.getId())
                                .with(user(mario.getEmail()))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("CANCELLED"))
                                .andExpect(jsonPath("$.statusMode").value("MANUAL"));

                eventRepository.flush();

                assertEquals(MedicalEventStatus.CANCELLED, event.getStatus());
                assertEquals(MedicalEventStatusMode.MANUAL, event.getStatusMode());
                assertEquals(newDate, event.getEventDate());
        }

        @Test
        void shouldAddAndPersistReport() throws Exception {
                MedicalEvent event = createReportTestEvent();

                mockMvc.perform(put("/api/medical-events/" + event.getId() + "/report")
                                .with(user(mario.getEmail()))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                    "reportText": "Esame nella norma"
                                                }
                                                """))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.reportText")
                                                .value("Esame nella norma"))
                                .andExpect(jsonPath("$.reportNotExpected")
                                                .value(false));

                eventRepository.flush();

                MedicalEvent saved = eventRepository.findById(event.getId())
                                .orElseThrow();

                assertEquals("Esame nella norma", saved.getReportText());
                assertFalse(saved.isReportNotExpected());
        }

        @Test
        void shouldUpdateExistingReport() throws Exception {
                MedicalEvent event = createReportTestEvent();
                event.updateReportText("Primo referto");

                mockMvc.perform(put("/api/medical-events/" + event.getId() + "/report")
                                .with(user(mario.getEmail()))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                    "reportText": "Referto aggiornato"
                                                }
                                                """))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.reportText")
                                                .value("Referto aggiornato"));

                eventRepository.flush();

                assertEquals("Referto aggiornato", event.getReportText());
        }

        @Test
        void shouldRejectBlankReport() throws Exception {
                MedicalEvent event = createReportTestEvent();

                mockMvc.perform(put("/api/medical-events/" + event.getId() + "/report")
                                .with(user(mario.getEmail()))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                    "reportText": "   "
                                                }
                                                """))
                                .andExpect(status().isBadRequest());

                assertNull(event.getReportText());
        }

        @Test
        void shouldDeleteReport() throws Exception {
                MedicalEvent event = createReportTestEvent();
                event.updateReportText("Referto da eliminare");

                mockMvc.perform(delete("/api/medical-events/" + event.getId() + "/report")
                                .with(user(mario.getEmail()))
                                .with(csrf()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.reportText").doesNotExist())
                                .andExpect(jsonPath("$.reportNotExpected").value(false));

                eventRepository.flush();

                assertNull(event.getReportText());
                assertFalse(event.isReportNotExpected());
        }

        @Test
        void shouldMarkReportAsNotExpected() throws Exception {
                MedicalEvent event = createReportTestEvent();
                event.updateReportText("Referto precedente");

                mockMvc.perform(patch("/api/medical-events/" + event.getId()
                                + "/report-not-expected")
                                .with(user(mario.getEmail()))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                    "reportNotExpected": true
                                                }
                                                """))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.reportNotExpected").value(true))
                                .andExpect(jsonPath("$.reportText").doesNotExist());

                eventRepository.flush();

                assertTrue(event.isReportNotExpected());
                assertNull(event.getReportText());
        }

        @Test
        void shouldRestoreReportExpectation() throws Exception {
                MedicalEvent event = createReportTestEvent();
                event.setReportNotExpected(true);

                mockMvc.perform(patch("/api/medical-events/" + event.getId()
                                + "/report-not-expected")
                                .with(user(mario.getEmail()))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                    "reportNotExpected": false
                                                }
                                                """))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.reportNotExpected").value(false));

                assertFalse(event.isReportNotExpected());
        }

        @Test
        void shouldRejectMissingReportNotExpectedValue() throws Exception {
                MedicalEvent event = createReportTestEvent();

                mockMvc.perform(patch("/api/medical-events/" + event.getId()
                                + "/report-not-expected")
                                .with(user(mario.getEmail()))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void shouldPreventUpdatingAnotherUsersReport() throws Exception {
                MedicalEvent event = createReportTestEvent();

                mockMvc.perform(put("/api/medical-events/" + event.getId() + "/report")
                                .with(user(giulia.getEmail()))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                    "reportText": "Modifica non autorizzata"
                                                }
                                                """))
                                .andExpect(status().isNotFound());

                assertNull(event.getReportText());
        }

        @Test
        void shouldPreventDeletingAnotherUsersReport() throws Exception {
                MedicalEvent event = createReportTestEvent();
                event.updateReportText("Referto privato");

                mockMvc.perform(delete("/api/medical-events/" + event.getId() + "/report")
                                .with(user(giulia.getEmail()))
                                .with(csrf()))
                                .andExpect(status().isNotFound());

                assertEquals("Referto privato", event.getReportText());
        }

        @Test
        void shouldPreventChangingAnotherUsersReportExpectation() throws Exception {
                MedicalEvent event = createReportTestEvent();

                mockMvc.perform(patch("/api/medical-events/" + event.getId()
                                + "/report-not-expected")
                                .with(user(giulia.getEmail()))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                    "reportNotExpected": true
                                                }
                                                """))
                                .andExpect(status().isNotFound());

                assertFalse(event.isReportNotExpected());
        }

        @Test
        void shouldRejectUnauthenticatedReportUpdate() throws Exception {
                MedicalEvent event = createReportTestEvent();

                mockMvc.perform(put("/api/medical-events/" + event.getId() + "/report")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                    "reportText": "Referto"
                                                }
                                                """))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        void shouldRejectReportUpdateForNonexistentEvent() throws Exception {
                mockMvc.perform(put("/api/medical-events/999999/report")
                                .with(user(mario.getEmail()))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                    "reportText": "Referto"
                                                }
                                                """))
                                .andExpect(status().isNotFound());
        }

        @Test
        void shouldReturnReportMissingForCompletedEvent() throws Exception {
                MedicalEvent event = createReportTestEvent();
                event.updateAutomaticStatus(MedicalEventStatus.COMPLETED);

                mockMvc.perform(get("/api/medical-events/" + event.getId())
                                .with(user(mario.getEmail())))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.reportMissing").value(true));
        }

        @Test
        void shouldClearReportMissingAfterAddingReport() throws Exception {
                MedicalEvent event = createReportTestEvent();
                event.updateAutomaticStatus(MedicalEventStatus.COMPLETED);

                mockMvc.perform(put("/api/medical-events/" + event.getId() + "/report")
                                .with(user(mario.getEmail()))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                    "reportText": "Esame nella norma"
                                                }
                                                """))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.reportMissing").value(false))
                                .andExpect(jsonPath("$.reportText")
                                                .value("Esame nella norma"));
        }

        @Test
        void shouldCreateEventWithOwnDoctorAndFacility() throws Exception {
                Doctor doctor = doctorRepository.save(
                                new Doctor(mario, "Dott. Rossi", "Endocrinologia"));

                Facility facility = facilityRepository.save(
                                new Facility(mario, "Ospedale Centrale", "Torino"));

                String request = """
                                {
                                    "serviceId": %d,
                                    "eventDate": "2026-12-10",
                                    "doctorId": %d,
                                    "facilityId": %d
                                }
                                """.formatted(
                                service.getId(), doctor.getId(), facility.getId());

                mockMvc.perform(post("/api/medical-events")
                                .with(user(mario.getEmail()))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.doctorId").value(doctor.getId()))
                                .andExpect(jsonPath("$.facilityId").value(facility.getId()));

                MedicalEvent event = eventRepository
                                .findByUserIdOrderByEventDateDescEventTimeDesc(mario.getId())
                                .get(0);

                assertEquals(doctor.getId(), event.getDoctor().getId());
                assertEquals(facility.getId(), event.getFacility().getId());
        }

        @Test
        void shouldRejectAnotherUsersDoctor() throws Exception {
                Doctor doctor = doctorRepository.save(
                                new Doctor(giulia, "Dott.ssa Bianchi", "Cardiologia"));

                String request = """
                                {
                                    "serviceId": %d,
                                    "eventDate": "2026-12-10",
                                    "doctorId": %d
                                }
                                """.formatted(service.getId(), doctor.getId());

                mockMvc.perform(post("/api/medical-events")
                                .with(user(mario.getEmail()))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request))
                                .andExpect(status().isNotFound());

                assertTrue(eventRepository
                                .findByUserIdOrderByEventDateDescEventTimeDesc(mario.getId())
                                .isEmpty());
        }

        @Test
        void shouldRejectAnotherUsersFacility() throws Exception {
                Facility facility = facilityRepository.save(
                                new Facility(giulia, "Clinica Privata", "Milano"));

                String request = """
                                {
                                    "serviceId": %d,
                                    "eventDate": "2026-12-10",
                                    "facilityId": %d
                                }
                                """.formatted(service.getId(), facility.getId());

                mockMvc.perform(post("/api/medical-events")
                                .with(user(mario.getEmail()))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request))
                                .andExpect(status().isNotFound());

                assertTrue(eventRepository
                                .findByUserIdOrderByEventDateDescEventTimeDesc(mario.getId())
                                .isEmpty());
        }

        @Test
        void shouldUpdateEventDoctorAndFacility() throws Exception {
                MedicalEvent event = eventRepository.save(
                                new MedicalEvent(
                                                mario, service, LocalDate.now(clock).plusDays(5),
                                                null, null, null, null));

                Doctor doctor = doctorRepository.save(
                                new Doctor(mario, "Dott. Verdi", "Neurologia"));

                Facility facility = facilityRepository.save(
                                new Facility(mario, "Centro Medico", "Torino"));

                String request = """
                                {
                                    "serviceId": %d,
                                    "eventDate": "%s",
                                    "doctorId": %d,
                                    "facilityId": %d
                                }
                                """.formatted(
                                service.getId(),
                                LocalDate.now(clock).plusDays(5),
                                doctor.getId(),
                                facility.getId());

                mockMvc.perform(put("/api/medical-events/" + event.getId())
                                .with(user(mario.getEmail()))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.doctorId").value(doctor.getId()))
                                .andExpect(jsonPath("$.facilityId").value(facility.getId()));

                eventRepository.flush();

                assertEquals(doctor.getId(), event.getDoctor().getId());
                assertEquals(facility.getId(), event.getFacility().getId());
        }

        @Test
        void shouldCreateEventWithDiagnosticOptions() throws Exception {
                MedicalService diagnosticService = serviceRepository.save(
                                new MedicalService(
                                                null,
                                                "Ecografia test opzioni",
                                                MedicalServiceCategory.DIAGNOSTIC,
                                                null));

                MedicalServiceOption option1 = optionRepository.save(
                                new MedicalServiceOption(
                                                diagnosticService,
                                                "Tiroide"));

                MedicalServiceOption option2 = optionRepository.save(
                                new MedicalServiceOption(
                                                diagnosticService,
                                                "Addome"));

                String request = """
                                {
                                    "serviceId": %d,
                                    "eventDate": "2026-12-10",
                                    "optionIds": [%d, %d]
                                }
                                """.formatted(
                                diagnosticService.getId(),
                                option1.getId(),
                                option2.getId());

                mockMvc.perform(post("/api/medical-events")
                                .with(user(mario.getEmail()))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.optionIds.length()").value(2))
                                .andExpect(jsonPath("$.optionIds")
                                                .value(org.hamcrest.Matchers.containsInAnyOrder(
                                                                option1.getId().intValue(),
                                                                option2.getId().intValue())));
        }

        @Test
        void shouldRejectOptionFromAnotherService() throws Exception {
                MedicalService diagnosticService = serviceRepository.save(
                                new MedicalService(
                                                null,
                                                "Ecografia test A",
                                                MedicalServiceCategory.DIAGNOSTIC,
                                                null));

                MedicalService otherService = serviceRepository.save(
                                new MedicalService(
                                                null,
                                                "Ecografia test B",
                                                MedicalServiceCategory.DIAGNOSTIC,
                                                null));

                MedicalServiceOption option = optionRepository.save(
                                new MedicalServiceOption(otherService, "Tiroide"));

                String request = """
                                {
                                    "serviceId": %d,
                                    "eventDate": "2026-12-10",
                                    "optionIds": [%d]
                                }
                                """.formatted(
                                diagnosticService.getId(),
                                option.getId());

                mockMvc.perform(post("/api/medical-events")
                                .with(user(mario.getEmail()))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void shouldRejectDuplicateDiagnosticOptions() throws Exception {
                MedicalService diagnosticService = serviceRepository.save(
                                new MedicalService(
                                                null,
                                                "Ecografia test duplicati",
                                                MedicalServiceCategory.DIAGNOSTIC,
                                                null));

                MedicalServiceOption option = optionRepository.save(
                                new MedicalServiceOption(diagnosticService, "Tiroide"));

                String request = """
                                {
                                    "serviceId": %d,
                                    "eventDate": "2026-12-10",
                                    "optionIds": [%d, %d]
                                }
                                """.formatted(
                                diagnosticService.getId(),
                                option.getId(),
                                option.getId());

                mockMvc.perform(post("/api/medical-events")
                                .with(user(mario.getEmail()))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void shouldRejectDiagnosticOptionsForSpecialistVisit() throws Exception {
                MedicalService diagnosticService = serviceRepository.save(
                                new MedicalService(
                                                null,
                                                "Ecografia test categoria",
                                                MedicalServiceCategory.DIAGNOSTIC,
                                                null));

                MedicalServiceOption option = optionRepository.save(
                                new MedicalServiceOption(diagnosticService, "Tiroide"));

                String request = """
                                {
                                    "serviceId": %d,
                                    "eventDate": "2026-12-10",
                                    "optionIds": [%d]
                                }
                                """.formatted(
                                service.getId(),
                                option.getId());

                mockMvc.perform(post("/api/medical-events")
                                .with(user(mario.getEmail()))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void shouldRejectMultipleOptionsForSingleSelectionService() {
                MedicalService service = serviceRepository.findAll().stream()
                                .filter(s -> s.getCategory() == MedicalServiceCategory.DIAGNOSTIC)
                                .filter(s -> s.getOptionSelectionMode() == OptionSelectionMode.SINGLE)
                                .findFirst()
                                .orElseThrow();

                List<MedicalServiceOption> options = optionRepository
                                .findByServiceIdAndActiveTrueOrderByNameAsc(service.getId());

                assertTrue(options.size() >= 2);

                CreateMedicalEventRequest request = new CreateMedicalEventRequest(
                                service.getId(),
                                LocalDate.of(2026, 12, 10),
                                null,
                                null,
                                null,
                                null,
                                List.of(options.get(0).getId(), options.get(1).getId()));

                ResponseStatusException exception = assertThrows(
                                ResponseStatusException.class,
                                () -> medicalEventService.createEvent(mario.getEmail(), request));

                assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        }

        @Test
        void shouldSaveAndRetrieveDiagnosticOptions() {
                MedicalService service = serviceRepository.findAll().stream()
                                .filter(s -> s.getCategory() == MedicalServiceCategory.DIAGNOSTIC)
                                .filter(s -> s.getOptionSelectionMode() == OptionSelectionMode.MULTIPLE)
                                .filter(s -> optionRepository
                                                .findByServiceIdAndActiveTrueOrderByNameAsc(s.getId()).size() >= 2)
                                .findFirst()
                                .orElseThrow();

                List<MedicalServiceOption> options = optionRepository
                                .findByServiceIdAndActiveTrueOrderByNameAsc(service.getId());

                List<Long> selectedIds = List.of(
                                options.get(0).getId(),
                                options.get(1).getId());

                MedicalEventResponse created = medicalEventService.createEvent(
                                mario.getEmail(),
                                new CreateMedicalEventRequest(
                                                service.getId(),
                                                LocalDate.of(2026, 12, 10),
                                                null,
                                                null,
                                                null,
                                                null,
                                                selectedIds));

                MedicalEventResponse retrieved = medicalEventService.getEvent(created.id(), mario.getEmail());

                assertEquals(2, retrieved.optionIds().size());
                assertTrue(retrieved.optionIds().containsAll(selectedIds));
        }

        @Test
        void shouldReplaceDiagnosticOptionsOnUpdate() {
                MedicalService service = serviceRepository.findAll().stream()
                                .filter(s -> s.getCategory() == MedicalServiceCategory.DIAGNOSTIC)
                                .filter(s -> s.getOptionSelectionMode() == OptionSelectionMode.MULTIPLE)
                                .filter(s -> optionRepository
                                                .findByServiceIdAndActiveTrueOrderByNameAsc(s.getId()).size() >= 2)
                                .findFirst()
                                .orElseThrow();

                List<MedicalServiceOption> options = optionRepository
                                .findByServiceIdAndActiveTrueOrderByNameAsc(service.getId());

                Long firstOptionId = options.get(0).getId();
                Long secondOptionId = options.get(1).getId();

                MedicalEventResponse created = medicalEventService.createEvent(
                                mario.getEmail(),
                                new CreateMedicalEventRequest(
                                                service.getId(),
                                                LocalDate.of(2026, 12, 10),
                                                null,
                                                null,
                                                null,
                                                null,
                                                List.of(firstOptionId)));

                MedicalEventResponse updated = medicalEventService.updateEvent(
                                created.id(),
                                mario.getEmail(),
                                new UpdateMedicalEventRequest(
                                                service.getId(),
                                                LocalDate.of(2026, 12, 11),
                                                null,
                                                null,
                                                null,
                                                null,
                                                List.of(secondOptionId)));

                assertEquals(List.of(secondOptionId), updated.optionIds());

                MedicalEventResponse retrieved = medicalEventService.getEvent(created.id(), mario.getEmail());

                assertEquals(List.of(secondOptionId), retrieved.optionIds());
        }

        @Test
        void shouldRemoveAllDiagnosticOptionsOnUpdate() {
                MedicalService service = serviceRepository.findAll().stream()
                                .filter(s -> s.getCategory() == MedicalServiceCategory.DIAGNOSTIC)
                                .filter(s -> !optionRepository
                                                .findByServiceIdAndActiveTrueOrderByNameAsc(s.getId()).isEmpty())
                                .findFirst()
                                .orElseThrow();

                Long optionId = optionRepository
                                .findByServiceIdAndActiveTrueOrderByNameAsc(service.getId())
                                .get(0).getId();

                MedicalEventResponse created = medicalEventService.createEvent(
                                mario.getEmail(),
                                new CreateMedicalEventRequest(
                                                service.getId(),
                                                LocalDate.of(2026, 12, 10),
                                                null,
                                                null,
                                                null,
                                                null,
                                                List.of(optionId)));

                MedicalEventResponse updated = medicalEventService.updateEvent(
                                created.id(),
                                mario.getEmail(),
                                new UpdateMedicalEventRequest(
                                                service.getId(),
                                                LocalDate.of(2026, 12, 10),
                                                null,
                                                null,
                                                null,
                                                null,
                                                List.of()));

                assertTrue(updated.optionIds().isEmpty());
                assertTrue(medicalEventService
                                .getEvent(created.id(), mario.getEmail())
                                .optionIds().isEmpty());
        }

        @Test
        void shouldRejectNonexistentDiagnosticOption() {
                MedicalService service = serviceRepository.findAll().stream()
                                .filter(s -> s.getCategory() == MedicalServiceCategory.DIAGNOSTIC)
                                .findFirst()
                                .orElseThrow();

                CreateMedicalEventRequest request = new CreateMedicalEventRequest(
                                service.getId(),
                                LocalDate.of(2026, 12, 10),
                                null,
                                null,
                                null,
                                null,
                                List.of(Long.MAX_VALUE));

                ResponseStatusException exception = assertThrows(
                                ResponseStatusException.class,
                                () -> medicalEventService.createEvent(mario.getEmail(), request));

                assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        }

        @Test
        void shouldRejectNullDiagnosticOptionId() {
                MedicalService service = serviceRepository.findAll().stream()
                                .filter(s -> s.getCategory() == MedicalServiceCategory.DIAGNOSTIC)
                                .findFirst()
                                .orElseThrow();

                List<Long> optionIds = new java.util.ArrayList<>();
                optionIds.add(null);

                CreateMedicalEventRequest request = new CreateMedicalEventRequest(
                                service.getId(),
                                LocalDate.of(2026, 12, 10),
                                null,
                                null,
                                null,
                                null,
                                optionIds);

                ResponseStatusException exception = assertThrows(
                                ResponseStatusException.class,
                                () -> medicalEventService.createEvent(mario.getEmail(), request));

                assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        }

        @Test
        void shouldReplaceDiagnosticOptionsWhenChangingService() {
                List<MedicalService> diagnosticServices = serviceRepository.findAll()
                                .stream()
                                .filter(s -> s.getCategory() == MedicalServiceCategory.DIAGNOSTIC)
                                .filter(s -> !optionRepository
                                                .findByServiceIdAndActiveTrueOrderByNameAsc(s.getId())
                                                .isEmpty())
                                .limit(2)
                                .toList();

                assertEquals(2, diagnosticServices.size());

                MedicalService firstService = diagnosticServices.get(0);
                MedicalService secondService = diagnosticServices.get(1);

                Long firstOptionId = optionRepository
                                .findByServiceIdAndActiveTrueOrderByNameAsc(firstService.getId())
                                .get(0).getId();

                Long secondOptionId = optionRepository
                                .findByServiceIdAndActiveTrueOrderByNameAsc(secondService.getId())
                                .get(0).getId();

                MedicalEventResponse created = medicalEventService.createEvent(
                                mario.getEmail(),
                                new CreateMedicalEventRequest(
                                                firstService.getId(),
                                                LocalDate.of(2026, 12, 10),
                                                null,
                                                null,
                                                null,
                                                null,
                                                List.of(firstOptionId)));

                assertEquals(
                                List.of(firstOptionId),
                                created.optionIds());

                MedicalEventResponse updated = medicalEventService.updateEvent(
                                created.id(),
                                mario.getEmail(),
                                new UpdateMedicalEventRequest(
                                                secondService.getId(),
                                                LocalDate.of(2026, 12, 11),
                                                null,
                                                null,
                                                null,
                                                null,
                                                List.of(secondOptionId)));

                assertEquals(secondService.getId(), updated.serviceId());
                assertEquals(List.of(secondOptionId), updated.optionIds());

                MedicalEventResponse retrieved = medicalEventService.getEvent(
                                created.id(),
                                mario.getEmail());

                assertEquals(secondService.getId(), retrieved.serviceId());
                assertEquals(List.of(secondOptionId), retrieved.optionIds());

                assertFalse(retrieved.optionIds().contains(firstOptionId));
        }

        private MedicalEventResponse createEventForFiltering(
                        Long serviceId,
                        LocalDate date) {
                return medicalEventService.createEvent(
                                mario.getEmail(),
                                new CreateMedicalEventRequest(
                                                serviceId,
                                                date,
                                                null,
                                                null,
                                                null,
                                                null,
                                                List.of()));
        }

        private Long getGlobalSpecialistServiceId() {
                return serviceRepository.findAll()
                                .stream()
                                .filter(service -> service.getUser() == null)
                                .filter(service -> service.getCategory() == MedicalServiceCategory.SPECIALIST)
                                .findFirst()
                                .orElseThrow(() -> new IllegalStateException(
                                                "No global specialist service found for testing"))
                                .getId();
        }

        @Test
        void shouldFilterMedicalEventsByCategory() {
                MedicalEventResponse created = createEventForFiltering(
                                getGlobalSpecialistServiceId(),
                                LocalDate.of(2026, 12, 10));

                MedicalEventFilter filter = new MedicalEventFilter(
                                MedicalServiceCategory.SPECIALIST,
                                null, null, null, null, null);

                List<MedicalEventResponse> results = medicalEventService.getFilteredEvents(mario.getEmail(), filter);

                assertTrue(results.stream().anyMatch(e -> e.id().equals(created.id())));

                assertTrue(results.stream().allMatch(
                                e -> e.category().equals(MedicalServiceCategory.SPECIALIST.name())));
        }

        @Test
        void shouldFilterMedicalEventsByStatus() {
                MedicalEventResponse created = createEventForFiltering(
                                getGlobalSpecialistServiceId(),
                                LocalDate.of(2026, 12, 10));

                MedicalEventFilter filter = new MedicalEventFilter(
                                null,
                                MedicalEventStatus.SCHEDULED,
                                null, null, null, null);

                List<MedicalEventResponse> results = medicalEventService.getFilteredEvents(mario.getEmail(), filter);

                assertTrue(results.stream().anyMatch(e -> e.id().equals(created.id())));

                assertTrue(results.stream().allMatch(
                                e -> e.status() == MedicalEventStatus.SCHEDULED));
        }

        @Test
        void shouldFilterMedicalEventsByDateRange() {
                MedicalEventResponse inside = createEventForFiltering(
                                getGlobalSpecialistServiceId(),
                                LocalDate.of(2026, 12, 10));

                MedicalEventResponse outside = createEventForFiltering(
                                getGlobalSpecialistServiceId(),
                                LocalDate.of(2027, 2, 10));

                MedicalEventFilter filter = new MedicalEventFilter(
                                null, null, null, null,
                                LocalDate.of(2026, 12, 1),
                                LocalDate.of(2026, 12, 31));

                List<MedicalEventResponse> results = medicalEventService.getFilteredEvents(mario.getEmail(), filter);

                assertTrue(results.stream().anyMatch(e -> e.id().equals(inside.id())));

                assertFalse(results.stream().anyMatch(e -> e.id().equals(outside.id())));
        }

        @Test
        void shouldRejectInvalidDateRange() {
                MedicalEventFilter filter = new MedicalEventFilter(
                                null, null, null, null,
                                LocalDate.of(2026, 12, 31),
                                LocalDate.of(2026, 12, 1));

                ResponseStatusException exception = assertThrows(
                                ResponseStatusException.class,
                                () -> medicalEventService.getFilteredEvents(mario.getEmail(), filter));

                assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        }

        @Test
        void shouldReturnAllOwnEventsWithoutFilters() {
                MedicalEventResponse created = createEventForFiltering(
                                getGlobalSpecialistServiceId(),
                                LocalDate.of(2026, 12, 10));

                MedicalEventFilter filter = new MedicalEventFilter(
                                null, null, null, null, null, null);

                List<MedicalEventResponse> results = medicalEventService.getFilteredEvents(mario.getEmail(), filter);

                assertTrue(results.stream().anyMatch(e -> e.id().equals(created.id())));
        }

        @Test
        void shouldKeepMedicalEventsPrivateWhenFiltering() {
                MedicalEventResponse created = createEventForFiltering(
                                getGlobalSpecialistServiceId(),
                                LocalDate.of(2026, 12, 10));

                MedicalEventFilter filter = new MedicalEventFilter(
                                null, null, null, null, null, null);

                List<MedicalEventResponse> giuliaResults = medicalEventService.getFilteredEvents(giulia.getEmail(),
                                filter);

                assertFalse(giuliaResults.stream()
                                .anyMatch(e -> e.id().equals(created.id())));
        }

        @Test
        void shouldReturnMedicalEventsOrderedByDateDescending() {
                MedicalEventResponse older = createEventForFiltering(
                                getGlobalSpecialistServiceId(),
                                LocalDate.of(2026, 12, 10));

                MedicalEventResponse newer = createEventForFiltering(
                                getGlobalSpecialistServiceId(),
                                LocalDate.of(2027, 1, 10));

                MedicalEventFilter filter = new MedicalEventFilter(
                                null, null, null, null, null, null);

                List<MedicalEventResponse> results = medicalEventService.getFilteredEvents(mario.getEmail(), filter);

                int olderIndex = -1;
                int newerIndex = -1;

                for (int i = 0; i < results.size(); i++) {
                        if (results.get(i).id().equals(older.id())) {
                                olderIndex = i;
                        }
                        if (results.get(i).id().equals(newer.id())) {
                                newerIndex = i;
                        }
                }

                assertTrue(olderIndex >= 0);
                assertTrue(newerIndex >= 0);
                assertTrue(newerIndex < olderIndex);
        }

        @Test
        void shouldFilterMedicalEventsByDoctor() {
                Doctor doctor1 = doctorRepository.save(
                                new Doctor(mario, "Dott. Rossi", "Endocrinologia"));

                Doctor doctor2 = doctorRepository.save(
                                new Doctor(mario, "Dott. Verdi", "Cardiologia"));

                MedicalEvent event1 = eventRepository.save(new MedicalEvent(
                                mario, service, LocalDate.of(2026, 12, 10),
                                null, doctor1, null, "Visita endocrinologica"));

                MedicalEvent event2 = eventRepository.save(new MedicalEvent(
                                mario, service, LocalDate.of(2026, 12, 11),
                                null, doctor2, null, "Visita cardiologica"));

                MedicalEventFilter filter = new MedicalEventFilter(
                                null, null, doctor1.getId(), null, null, null);

                List<MedicalEventResponse> results = medicalEventService.getFilteredEvents(mario.getEmail(), filter);

                assertEquals(1, results.size());
                assertEquals(event1.getId(), results.get(0).id());
                assertFalse(results.stream()
                                .anyMatch(e -> e.id().equals(event2.getId())));
        }

        @Test
        void shouldFilterMedicalEventsByFacility() {
                Facility facility1 = facilityRepository.save(
                                new Facility(mario, "Ospedale Centrale", "Torino"));

                Facility facility2 = facilityRepository.save(
                                new Facility(mario, "Centro Medico", "Torino"));

                MedicalEvent event1 = eventRepository.save(new MedicalEvent(
                                mario, service, LocalDate.of(2026, 12, 10),
                                null, null, facility1, "Visita in ospedale"));

                MedicalEvent event2 = eventRepository.save(new MedicalEvent(
                                mario, service, LocalDate.of(2026, 12, 11),
                                null, null, facility2, "Visita al centro medico"));

                MedicalEventFilter filter = new MedicalEventFilter(
                                null, null, null, facility1.getId(), null, null);

                List<MedicalEventResponse> results = medicalEventService.getFilteredEvents(mario.getEmail(), filter);

                assertEquals(1, results.size());
                assertEquals(event1.getId(), results.get(0).id());
                assertFalse(results.stream()
                                .anyMatch(e -> e.id().equals(event2.getId())));
        }

        @Test
        void shouldCombineMedicalEventFilters() {
                MedicalEventResponse matching = createEventForFiltering(
                                service.getId(),
                                LocalDate.of(2026, 12, 10));

                MedicalEventResponse outsideRange = createEventForFiltering(
                                service.getId(),
                                LocalDate.of(2027, 2, 10));

                MedicalEventFilter filter = new MedicalEventFilter(
                                MedicalServiceCategory.SPECIALIST,
                                MedicalEventStatus.SCHEDULED,
                                null,
                                null,
                                LocalDate.of(2026, 12, 1),
                                LocalDate.of(2026, 12, 31));

                List<MedicalEventResponse> results = medicalEventService.getFilteredEvents(mario.getEmail(), filter);

                assertEquals(1, results.size());
                assertEquals(matching.id(), results.get(0).id());
                assertFalse(results.stream()
                                .anyMatch(e -> e.id().equals(outsideRange.id())));
        }

        @Test
        void shouldFilterMedicalEventsThroughHttp() throws Exception {
                MedicalEventResponse matching = createEventForFiltering(
                                service.getId(),
                                LocalDate.of(2026, 12, 10));

                createEventForFiltering(
                                service.getId(),
                                LocalDate.of(2027, 2, 10));

                mockMvc.perform(get("/api/medical-events")
                                .with(user(mario.getEmail()))
                                .param("category", "SPECIALIST")
                                .param("status", "SCHEDULED")
                                .param("fromDate", "2026-12-01")
                                .param("toDate", "2026-12-31"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(1))
                                .andExpect(jsonPath("$[0].id").value(matching.id()));
        }

        @Test
        void shouldRejectInvalidDateRangeThroughHttp() throws Exception {
                mockMvc.perform(get("/api/medical-events")
                                .with(user(mario.getEmail()))
                                .param("fromDate", "2026-12-31")
                                .param("toDate", "2026-12-01"))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void shouldKeepMedicalEventsPrivateWhenFilteringByDoctor() {
                Doctor doctor = doctorRepository.save(
                                new Doctor(mario, "Dott. Rossi", "Endocrinologia"));

                MedicalEvent event = eventRepository.save(new MedicalEvent(
                                mario, service, LocalDate.of(2026, 12, 10),
                                null, doctor, null, "Visita privata"));

                MedicalEventFilter filter = new MedicalEventFilter(
                                null, null, doctor.getId(), null, null, null);

                List<MedicalEventResponse> results = medicalEventService.getFilteredEvents(giulia.getEmail(), filter);

                assertTrue(results.isEmpty());
                assertFalse(results.stream()
                                .anyMatch(e -> e.id().equals(event.getId())));
        }

        @Test
        void shouldDeleteMedicalEvent() throws Exception {
                MedicalEvent event = eventRepository.save(new MedicalEvent(
                                mario,
                                service,
                                LocalDate.now(clock).plusDays(10),
                                null,
                                null,
                                null,
                                "Prestazione da eliminare"));

                Long eventId = event.getId();

                mockMvc.perform(delete("/api/medical-events/" + eventId)
                                .with(user(mario.getEmail()))
                                .with(csrf()))
                                .andExpect(status().isNoContent());

                eventRepository.flush();

                assertFalse(eventRepository.findById(eventId).isPresent());
        }

        @Test
        void shouldPreventDeletingAnotherUsersEvent() throws Exception {
                MedicalEvent event = eventRepository.save(new MedicalEvent(
                                mario,
                                service,
                                LocalDate.now(clock).plusDays(10),
                                null,
                                null,
                                null,
                                "Prestazione privata"));

                mockMvc.perform(delete("/api/medical-events/" + event.getId())
                                .with(user(giulia.getEmail()))
                                .with(csrf()))
                                .andExpect(status().isNotFound());

                assertTrue(eventRepository.findById(event.getId()).isPresent());
        }

        @Test
        void shouldRejectDeletingNonexistentEvent() throws Exception {
                mockMvc.perform(delete("/api/medical-events/999999")
                                .with(user(mario.getEmail()))
                                .with(csrf()))
                                .andExpect(status().isNotFound());
        }

        @Test
        void shouldRejectUnauthenticatedEventDeletion() throws Exception {
                MedicalEvent event = eventRepository.save(new MedicalEvent(
                                mario,
                                service,
                                LocalDate.now(clock).plusDays(10),
                                null,
                                null,
                                null,
                                "Prestazione protetta"));

                mockMvc.perform(delete("/api/medical-events/" + event.getId())
                                .with(csrf()))
                                .andExpect(status().isUnauthorized());

                assertTrue(eventRepository.findById(event.getId()).isPresent());
        }

        @Test
        void shouldDeleteMedicalEventWithDiagnosticOptions() throws Exception {
                MedicalService diagnosticService = serviceRepository.save(
                                new MedicalService(
                                                null,
                                                "Ecografia test eliminazione",
                                                MedicalServiceCategory.DIAGNOSTIC,
                                                null));

                MedicalServiceOption option = optionRepository.save(
                                new MedicalServiceOption(
                                                diagnosticService,
                                                "Tiroide"));

                MedicalEventResponse created = medicalEventService.createEvent(
                                mario.getEmail(),
                                new CreateMedicalEventRequest(
                                                diagnosticService.getId(),
                                                LocalDate.now(clock).plusDays(10),
                                                null,
                                                null,
                                                null,
                                                null,
                                                List.of(option.getId())));

                Long eventId = created.id();

                assertTrue(eventOptionRepository.existsByMedicalEventId(eventId));

                mockMvc.perform(delete("/api/medical-events/" + eventId)
                                .with(user(mario.getEmail()))
                                .with(csrf()))
                                .andExpect(status().isNoContent());

                eventRepository.flush();

                assertFalse(eventRepository.findById(eventId).isPresent());
                assertFalse(eventOptionRepository.existsByMedicalEventId(eventId));

                assertTrue(serviceRepository.findById(diagnosticService.getId()).isPresent());
                assertTrue(optionRepository.findById(option.getId()).isPresent());
        }

        @Test
        void shouldAutomaticallyCompletePastEventWhenRetrieved() {
                MedicalEvent event = eventRepository.save(new MedicalEvent(
                                mario, service, LocalDate.now(clock).minusDays(5),
                                null, null, null, "Visita passata"));

                assertEquals(MedicalEventStatus.SCHEDULED, event.getStatus());

                MedicalEventResponse response = medicalEventService.getEvent(event.getId(), mario.getEmail());

                assertEquals(MedicalEventStatus.COMPLETED, response.status());
                assertEquals(MedicalEventStatusMode.AUTO, response.statusMode());
        }

        @Test
        void shouldKeepCancelledEventCancelledWhenRetrieved() {
                MedicalEvent event = new MedicalEvent(
                                mario, service, LocalDate.now(clock).minusDays(5),
                                null, null, null, "Visita annullata");

                event.cancel();
                event = eventRepository.save(event);

                MedicalEventResponse response = medicalEventService.getEvent(event.getId(), mario.getEmail());

                assertEquals(MedicalEventStatus.CANCELLED, response.status());
                assertEquals(MedicalEventStatusMode.MANUAL, response.statusMode());
        }

        @Test
        void shouldFilterAutomaticallyCompletedEvents() {
                MedicalEvent event = eventRepository.save(new MedicalEvent(
                                mario, service, LocalDate.now(clock).minusDays(5),
                                null, null, null, "Visita da completare"));

                MedicalEventFilter filter = new MedicalEventFilter(
                                null, MedicalEventStatus.COMPLETED,
                                null, null, null, null);

                List<MedicalEventResponse> results = medicalEventService.getFilteredEvents(mario.getEmail(), filter);

                assertTrue(results.stream()
                                .anyMatch(e -> e.id().equals(event.getId())));

                assertTrue(results.stream()
                                .allMatch(e -> e.status() == MedicalEventStatus.COMPLETED));
        }

        @Test
        void shouldSynchronizeOnlyAuthenticatedUsersEvents() {
                MedicalEvent marioEvent = eventRepository.save(new MedicalEvent(
                                mario, service, LocalDate.now(clock).minusDays(5),
                                null, null, null, "Visita di Mario"));

                MedicalEvent giuliaEvent = eventRepository.save(new MedicalEvent(
                                giulia, service, LocalDate.now(clock).minusDays(5),
                                null, null, null, "Visita di Giulia"));

                MedicalEventFilter filter = new MedicalEventFilter(
                                null, null, null, null, null, null);

                medicalEventService.getFilteredEvents(mario.getEmail(), filter);

                assertEquals(MedicalEventStatus.COMPLETED, marioEvent.getStatus());
                assertEquals(MedicalEventStatus.SCHEDULED, giuliaEvent.getStatus());
        }

        @Test
        void shouldPersistAutomaticallyUpdatedStatus() {
                MedicalEvent event = eventRepository.save(new MedicalEvent(
                                mario, service, LocalDate.now(clock).minusDays(5),
                                null, null, null, "Visita da aggiornare"));

                Long eventId = event.getId();

                medicalEventService.getEvent(eventId, mario.getEmail());

                eventRepository.flush();

                MedicalEvent persisted = eventRepository.findById(eventId)
                                .orElseThrow();

                assertEquals(MedicalEventStatus.COMPLETED, persisted.getStatus());
        }

        @Test
        void shouldReturnAutomaticallyCompletedEventsThroughHttp() throws Exception {
                MedicalEvent event = eventRepository.save(new MedicalEvent(
                                mario, service, LocalDate.now(clock).minusDays(5),
                                null, null, null, "Visita passata HTTP"));

                mockMvc.perform(get("/api/medical-events")
                                .with(user(mario.getEmail()))
                                .param("status", "COMPLETED"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(1))
                                .andExpect(jsonPath("$[0].id").value(event.getId()))
                                .andExpect(jsonPath("$[0].status").value("COMPLETED"));
        }

}
