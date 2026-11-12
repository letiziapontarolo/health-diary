package com.healthdiary.backend.medical.service;

import com.healthdiary.backend.auth.entity.User;
import com.healthdiary.backend.auth.repository.UserRepository;
import com.healthdiary.backend.medical.dto.CreateMedicalEventRequest;
import com.healthdiary.backend.medical.dto.MedicalEventResponse;
import com.healthdiary.backend.medical.entity.*;
import com.healthdiary.backend.medical.repository.*;
import com.healthdiary.backend.medical.dto.MedicalEventFilter;
import org.springframework.data.jpa.domain.Specification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MedicalEventServiceTest {

        private static final String EMAIL = "mario@test.com";
        private static final Long USER_ID = 1L;
        private static final Long SERVICE_ID = 10L;

        @Mock
        private MedicalEventRepository eventRepository;

        @Mock
        private MedicalServiceRepository serviceRepository;

        @Mock
        private MedicalServiceOptionRepository optionRepository;

        @Mock
        private DoctorRepository doctorRepository;

        @Mock
        private FacilityRepository facilityRepository;

        @Mock
        private UserRepository userRepository;

        @Mock
        private Clock clock;

        @Mock
        private MedicalEventStatusCalculator statusCalculator;

        @InjectMocks
        private MedicalEventService medicalEventService;

        @Mock
        private MedicalEventOptionRepository eventOptionRepository;

        private User user;

        @BeforeEach
        void setUp() {
                user = mock(User.class);

                lenient().when(eventOptionRepository
                                .findByMedicalEventIdOrderByOptionNameAsc(anyLong()))
                                .thenReturn(List.of());
        }

        private void mockAuthenticatedUser() {
                when(userRepository.findByEmailIgnoreCase(EMAIL))
                                .thenReturn(Optional.of(user));

                when(user.getId()).thenReturn(USER_ID);
        }

        private MedicalService mockAccessibleService() {
                MedicalService service = mock(MedicalService.class);

                when(serviceRepository.findById(SERVICE_ID))
                                .thenReturn(Optional.of(service));

                when(service.isActive()).thenReturn(true);

                return service;
        }

        private CreateMedicalEventRequest mockRequest() {
                return new CreateMedicalEventRequest(
                                SERVICE_ID,
                                LocalDate.of(2026, 11, 20),
                                LocalTime.of(10, 30),
                                null,
                                null,
                                "Controllo periodico",
                                null);
        }

        private void assertStatus(
                        HttpStatus expected,
                        Runnable action) {

                ResponseStatusException exception = assertThrows(
                                ResponseStatusException.class,
                                action::run);

                assertEquals(expected, exception.getStatusCode());
        }

        private MedicalService mockServiceForRestore() {
                MedicalService service = mock(MedicalService.class);

                when(service.getCategory())
                                .thenReturn(MedicalServiceCategory.SPECIALIST);

                return service;
        }

        // User validation

        @Test
        void createEventRejectsUnknownUser() {
                when(userRepository.findByEmailIgnoreCase(EMAIL))
                                .thenReturn(Optional.empty());

                CreateMedicalEventRequest request = mock(CreateMedicalEventRequest.class);

                assertStatus(HttpStatus.NOT_FOUND,
                                () -> medicalEventService.createEvent(
                                                EMAIL, request));

                verify(eventRepository, never()).save(any());
        }

        @Test
        void getEventsRejectsUnknownUser() {
                when(userRepository.findByEmailIgnoreCase(EMAIL))
                                .thenReturn(Optional.empty());

                assertStatus(HttpStatus.NOT_FOUND,
                                () -> medicalEventService.getEvents(EMAIL));
        }

        // Event creation

        @Test
        void createEventRejectsNonexistentService() {
                mockAuthenticatedUser();

                when(serviceRepository.findById(SERVICE_ID))
                                .thenReturn(Optional.empty());

                CreateMedicalEventRequest request = mockRequest();

                assertStatus(HttpStatus.NOT_FOUND,
                                () -> medicalEventService.createEvent(
                                                EMAIL, request));

                verify(eventRepository, never()).save(any());
        }

        @Test
        void createEventRejectsAnotherUsersService() {
                mockAuthenticatedUser();

                MedicalService service = mock(MedicalService.class);
                User otherUser = mock(User.class);

                when(serviceRepository.findById(SERVICE_ID))
                                .thenReturn(Optional.of(service));

                when(service.getUser()).thenReturn(otherUser);
                when(otherUser.getId()).thenReturn(2L);

                CreateMedicalEventRequest request = mockRequest();

                assertStatus(HttpStatus.NOT_FOUND,
                                () -> medicalEventService.createEvent(
                                                EMAIL, request));

                verify(eventRepository, never()).save(any());
        }

        @Test
        void createEventRejectsArchivedService() {
                mockAuthenticatedUser();

                MedicalService service = mock(MedicalService.class);

                when(serviceRepository.findById(SERVICE_ID))
                                .thenReturn(Optional.of(service));

                CreateMedicalEventRequest request = mockRequest();

                assertStatus(HttpStatus.BAD_REQUEST,
                                () -> medicalEventService.createEvent(
                                                EMAIL, request));

                verify(eventRepository, never()).save(any());
        }

        @Test
        void createEventRejectsUnknownDoctor() {
                mockAuthenticatedUser();
                mockAccessibleService();

                CreateMedicalEventRequest request = new CreateMedicalEventRequest(
                                SERVICE_ID,
                                LocalDate.of(2026, 11, 20),
                                null,
                                20L,
                                null,
                                null,
                                null);

                when(doctorRepository.findByIdAndUserId(20L, USER_ID))
                                .thenReturn(Optional.empty());

                assertStatus(HttpStatus.NOT_FOUND,
                                () -> medicalEventService.createEvent(
                                                EMAIL, request));

                verify(eventRepository, never()).save(any());
        }

        @Test
        void createEventRejectsUnknownFacility() {
                mockAuthenticatedUser();
                mockAccessibleService();

                CreateMedicalEventRequest request = new CreateMedicalEventRequest(
                                SERVICE_ID,
                                LocalDate.of(2026, 11, 20),
                                null,
                                null,
                                30L,
                                null,
                                null);

                when(facilityRepository.findByIdAndUserId(30L, USER_ID))
                                .thenReturn(Optional.empty());

                assertStatus(HttpStatus.NOT_FOUND,
                                () -> medicalEventService.createEvent(
                                                EMAIL, request));

                verify(eventRepository, never()).save(any());
        }

        @Test
        void createEventRejectsOptionsForNonDiagnosticService() {
                mockAuthenticatedUser();
                mockAccessibleService();

                CreateMedicalEventRequest request = new CreateMedicalEventRequest(
                                SERVICE_ID,
                                LocalDate.of(2026, 11, 20),
                                null,
                                null,
                                null,
                                null,
                                List.of(5L));

                assertStatus(HttpStatus.BAD_REQUEST,
                                () -> medicalEventService.createEvent(
                                                EMAIL, request));

                verify(eventRepository, never()).save(any());
        }

        @Test
        void createEventSavesValidEvent() {
                mockAuthenticatedUser();

                MedicalService service = mockAccessibleService();

                when(service.getId()).thenReturn(SERVICE_ID);
                when(service.getName()).thenReturn("Visita di controllo");
                when(service.getCategory())
                                .thenReturn(MedicalServiceCategory.SPECIALIST);

                CreateMedicalEventRequest request = mockRequest();

                when(statusCalculator.calculate(
                                request.eventDate(),
                                request.eventTime())).thenReturn(MedicalEventStatus.SCHEDULED);

                when(eventRepository.save(any(MedicalEvent.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                MedicalEventResponse response = medicalEventService.createEvent(EMAIL, request);

                assertNotNull(response);
                assertEquals(SERVICE_ID, response.serviceId());
                assertEquals("Visita di controllo", response.serviceName());
                assertEquals(LocalDate.of(2026, 11, 20),
                                response.eventDate());

                verify(eventRepository).save(any(MedicalEvent.class));

                verify(statusCalculator).calculate(
                                LocalDate.of(2026, 11, 20),
                                LocalTime.of(10, 30));
        }

        // Event list

        @Test
        void getEventsReturnsEmptyList() {
                mockAuthenticatedUser();

                when(eventRepository
                                .findByUserIdOrderByEventDateDescEventTimeDesc(USER_ID))
                                .thenReturn(List.of());

                List<MedicalEventResponse> result = medicalEventService.getEvents(EMAIL);

                assertTrue(result.isEmpty());
        }

        @Test
        void getEventsFiltersByAuthenticatedUser() {
                mockAuthenticatedUser();

                when(eventRepository
                                .findByUserIdOrderByEventDateDescEventTimeDesc(USER_ID))
                                .thenReturn(List.of());

                medicalEventService.getEvents(EMAIL);

                verify(eventRepository)
                                .findByUserIdOrderByEventDateDescEventTimeDesc(USER_ID);
        }

        // Event detail

        @Test
        void getEventRejectsNonexistentEvent() {
                mockAuthenticatedUser();

                when(eventRepository.findByIdAndUserId(999L, USER_ID))
                                .thenReturn(Optional.empty());

                assertStatus(HttpStatus.NOT_FOUND,
                                () -> medicalEventService.getEvent(999L, EMAIL));
        }

        @Test
        void getEventRejectsAnotherUsersEvent() {
                mockAuthenticatedUser();

                when(eventRepository.findByIdAndUserId(42L, USER_ID))
                                .thenReturn(Optional.empty());

                assertStatus(HttpStatus.NOT_FOUND,
                                () -> medicalEventService.getEvent(42L, EMAIL));

                verify(eventRepository).findByIdAndUserId(42L, USER_ID);
        }

        @Test
        void getEventReturnsExistingEvent() {
                mockAuthenticatedUser();

                MedicalEvent event = mock(MedicalEvent.class);
                MedicalService service = mock(MedicalService.class);

                when(eventRepository.findByIdAndUserId(42L, USER_ID))
                                .thenReturn(Optional.of(event));

                when(event.getId()).thenReturn(42L);
                when(event.getService()).thenReturn(service);

                when(service.getId()).thenReturn(SERVICE_ID);
                when(service.getName()).thenReturn("Ecografia tiroidea");
                when(service.getCategory())
                                .thenReturn(MedicalServiceCategory.DIAGNOSTIC);

                MedicalEventResponse response = medicalEventService.getEvent(42L, EMAIL);

                assertEquals(42L, response.id());
                assertEquals("Ecografia tiroidea",
                                response.serviceName());
        }

        @Test
        void restoreTodayEventWithoutTimeAsScheduled() {
                mockAuthenticatedUser();

                MedicalEvent event = new MedicalEvent(
                                user,
                                mockServiceForRestore(),
                                LocalDate.of(2026, 11, 12),
                                null,
                                null,
                                null,
                                null);
                event.cancel();
                when(statusCalculator.calculate(
                                LocalDate.of(2026, 11, 12),
                                null)).thenReturn(MedicalEventStatus.SCHEDULED);

                when(eventRepository.findByIdAndUserId(42L, USER_ID))
                                .thenReturn(Optional.of(event));

                MedicalEventResponse response = medicalEventService.restoreEvent(42L, EMAIL);

                assertEquals(MedicalEventStatus.SCHEDULED, response.status());
                assertEquals(MedicalEventStatusMode.AUTO, response.statusMode());
        }

        @Test
        void restoreTodayFutureEventAsScheduled() {
                mockAuthenticatedUser();

                MedicalEvent event = new MedicalEvent(
                                user,
                                mockServiceForRestore(),
                                LocalDate.of(2026, 11, 12),
                                LocalTime.of(16, 0),
                                null,
                                null,
                                null);
                event.cancel();
                when(statusCalculator.calculate(
                                LocalDate.of(2026, 11, 12),
                                LocalTime.of(16, 0))).thenReturn(MedicalEventStatus.SCHEDULED);

                when(eventRepository.findByIdAndUserId(42L, USER_ID))
                                .thenReturn(Optional.of(event));

                MedicalEventResponse response = medicalEventService.restoreEvent(42L, EMAIL);

                assertEquals(MedicalEventStatus.SCHEDULED, response.status());
                assertEquals(MedicalEventStatusMode.AUTO, response.statusMode());
        }

        @Test
        void restoreTodayPastEventAsCompleted() {
                mockAuthenticatedUser();

                MedicalEvent event = new MedicalEvent(
                                user,
                                mockServiceForRestore(),
                                LocalDate.of(2026, 11, 12),
                                LocalTime.of(12, 0),
                                null,
                                null,
                                null);
                event.cancel();
                when(statusCalculator.calculate(
                                LocalDate.of(2026, 11, 12),
                                LocalTime.of(12, 0))).thenReturn(MedicalEventStatus.COMPLETED);

                when(eventRepository.findByIdAndUserId(42L, USER_ID))
                                .thenReturn(Optional.of(event));

                MedicalEventResponse response = medicalEventService.restoreEvent(42L, EMAIL);

                assertEquals(MedicalEventStatus.COMPLETED, response.status());
                assertEquals(MedicalEventStatusMode.AUTO, response.statusMode());
        }

        @Test
        void restoreTodayEventAtExactTimeAsCompleted() {
                mockAuthenticatedUser();

                MedicalEvent event = new MedicalEvent(
                                user,
                                mockServiceForRestore(),
                                LocalDate.of(2026, 11, 12),
                                LocalTime.of(14, 0),
                                null,
                                null,
                                null);
                event.cancel();
                when(statusCalculator.calculate(
                                LocalDate.of(2026, 11, 12),
                                LocalTime.of(14, 0))).thenReturn(MedicalEventStatus.COMPLETED);

                when(eventRepository.findByIdAndUserId(42L, USER_ID))
                                .thenReturn(Optional.of(event));

                MedicalEventResponse response = medicalEventService.restoreEvent(42L, EMAIL);

                assertEquals(MedicalEventStatus.COMPLETED, response.status());
                assertEquals(MedicalEventStatusMode.AUTO, response.statusMode());
        }

        @Test
        void createPastEventAsCompleted() {
                mockAuthenticatedUser();

                MedicalService service = mockAccessibleService();

                when(service.getCategory())
                                .thenReturn(MedicalServiceCategory.SPECIALIST);

                CreateMedicalEventRequest request = new CreateMedicalEventRequest(
                                SERVICE_ID,
                                LocalDate.of(2026, 11, 10),
                                LocalTime.of(10, 30),
                                null,
                                null,
                                null,
                                null);

                when(statusCalculator.calculate(
                                request.eventDate(),
                                request.eventTime())).thenReturn(MedicalEventStatus.COMPLETED);

                when(eventRepository.save(any(MedicalEvent.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                MedicalEventResponse response = medicalEventService.createEvent(EMAIL, request);

                assertEquals(MedicalEventStatus.COMPLETED, response.status());
                assertEquals(MedicalEventStatusMode.AUTO, response.statusMode());
        }

        @Test
        void getFilteredEventsSynchronizesOnlyScheduledAutomaticEvents() {
                mockAuthenticatedUser();

                when(eventRepository.findByUserIdAndStatusAndStatusMode(
                                USER_ID,
                                MedicalEventStatus.SCHEDULED,
                                MedicalEventStatusMode.AUTO))
                                .thenReturn(List.of());

                MedicalEventFilter filter = new MedicalEventFilter(
                                null, null, null, null, null, null);

                medicalEventService.getFilteredEvents(EMAIL, filter);

                verify(eventRepository).findByUserIdAndStatusAndStatusMode(
                                USER_ID,
                                MedicalEventStatus.SCHEDULED,
                                MedicalEventStatusMode.AUTO);

                verify(eventRepository).flush();
        }

        @Test
        void getFilteredEventsCompletesScheduledAutomaticEvents() {
                mockAuthenticatedUser();

                MedicalEvent event = new MedicalEvent(
                                user,
                                mock(MedicalService.class),
                                LocalDate.of(2026, 11, 10),
                                null,
                                null,
                                null,
                                "Prestazione da completare");

                when(eventRepository.findByUserIdAndStatusAndStatusMode(
                                USER_ID,
                                MedicalEventStatus.SCHEDULED,
                                MedicalEventStatusMode.AUTO))
                                .thenReturn(List.of(event));

                when(statusCalculator.calculate(
                                LocalDate.of(2026, 11, 10),
                                null))
                                .thenReturn(MedicalEventStatus.COMPLETED);

                MedicalEventFilter filter = new MedicalEventFilter(
                                null, MedicalEventStatus.COMPLETED,
                                null, null, null, null);

                medicalEventService.getFilteredEvents(EMAIL, filter);

                assertEquals(MedicalEventStatus.COMPLETED, event.getStatus());
                assertEquals(MedicalEventStatusMode.AUTO, event.getStatusMode());

                verify(eventRepository).flush();
        }

}
