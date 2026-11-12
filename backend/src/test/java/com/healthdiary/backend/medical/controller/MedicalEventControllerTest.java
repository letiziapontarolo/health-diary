
package com.healthdiary.backend.medical.controller;

import com.healthdiary.backend.config.SecurityConfig;
import com.healthdiary.backend.medical.service.MedicalEventService;
import com.healthdiary.backend.medical.dto.MedicalEventFilter;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MedicalEventController.class)
@Import(SecurityConfig.class)
class MedicalEventControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private MedicalEventService medicalEventService;

        private static final String BASE_URL = "/api/medical-events";

        private static final String VALID_REQUEST = """
                        {
                            "serviceId": 1,
                            "eventDate": "2026-11-20",
                            "eventTime": "10:30",
                            "notes": "Visita di controllo"
                        }
                        """;

        // Authentication

        @Test
        void shouldRejectUnauthenticatedListRequest() throws Exception {
                mockMvc.perform(get(BASE_URL))
                                .andExpect(status().isUnauthorized());

                verifyNoInteractions(medicalEventService);
        }

        @Test
        void shouldRejectUnauthenticatedDetailRequest() throws Exception {
                mockMvc.perform(get(BASE_URL + "/1"))
                                .andExpect(status().isUnauthorized());

                verifyNoInteractions(medicalEventService);
        }

        @Test
        void shouldRejectUnauthenticatedCreateRequest() throws Exception {
                mockMvc.perform(post(BASE_URL)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(VALID_REQUEST))
                                .andExpect(status().isUnauthorized());

                verifyNoInteractions(medicalEventService);
        }

        // List events

        @Test
        void shouldReturnEmptyEventList() throws Exception {
                MedicalEventFilter emptyFilter = new MedicalEventFilter(
                                null, null, null, null, null, null);

                when(medicalEventService.getFilteredEvents(
                                eq("mario@test.com"),
                                eq(emptyFilter))).thenReturn(List.of());

                mockMvc.perform(get(BASE_URL)
                                .with(user("mario@test.com")))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(0));

                verify(medicalEventService).getFilteredEvents(
                                eq("mario@test.com"),
                                eq(emptyFilter));
        }

        @Test
        void shouldUseAuthenticatedUserForEventList() throws Exception {
                MedicalEventFilter emptyFilter = new MedicalEventFilter(
                                null, null, null, null, null, null);

                when(medicalEventService.getFilteredEvents(
                                eq("giulia@test.com"),
                                eq(emptyFilter))).thenReturn(List.of());

                mockMvc.perform(get(BASE_URL)
                                .with(user("giulia@test.com")))
                                .andExpect(status().isOk());

                verify(medicalEventService).getFilteredEvents(
                                eq("giulia@test.com"),
                                eq(emptyFilter));

                verify(medicalEventService, never())
                                .getFilteredEvents(
                                                eq("mario@test.com"),
                                                any(MedicalEventFilter.class));
        }

        // Event detail

        @Test
        void shouldUseAuthenticatedUserForEventDetail() throws Exception {
                mockMvc.perform(get(BASE_URL + "/42")
                                .with(user("mario@test.com")))
                                .andExpect(status().isOk());

                verify(medicalEventService)
                                .getEvent(42L, "mario@test.com");
        }

        @Test
        void shouldReturnNotFoundForMissingEvent() throws Exception {
                when(medicalEventService.getEvent(999L, "mario@test.com"))
                                .thenThrow(new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "Medical event not found"));

                mockMvc.perform(get(BASE_URL + "/999")
                                .with(user("mario@test.com")))
                                .andExpect(status().isNotFound());
        }

        @Test
        void shouldReturnNotFoundForAnotherUsersEvent() throws Exception {
                when(medicalEventService.getEvent(42L, "giulia@test.com"))
                                .thenThrow(new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "Medical event not found"));

                mockMvc.perform(get(BASE_URL + "/42")
                                .with(user("giulia@test.com")))
                                .andExpect(status().isNotFound());
        }

        @Test
        void shouldRejectInvalidEventId() throws Exception {
                mockMvc.perform(get(BASE_URL + "/invalid")
                                .with(user("mario@test.com")))
                                .andExpect(status().isBadRequest());

                verifyNoInteractions(medicalEventService);
        }

        // Create event

        @Test
        void shouldCreateMedicalEvent() throws Exception {
                mockMvc.perform(post(BASE_URL)
                                .with(user("mario@test.com"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(VALID_REQUEST))
                                .andExpect(status().isCreated());

                verify(medicalEventService)
                                .createEvent(eq("mario@test.com"), any());
        }

        @Test
        void shouldRejectMissingCsrfToken() throws Exception {
                mockMvc.perform(post(BASE_URL)
                                .with(user("mario@test.com"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(VALID_REQUEST))
                                .andExpect(status().isForbidden());

                verifyNoInteractions(medicalEventService);
        }

        @Test
        void shouldRejectMissingServiceId() throws Exception {
                mockMvc.perform(post(BASE_URL)
                                .with(user("mario@test.com"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                    "eventDate": "2026-11-20"
                                                }
                                                """))
                                .andExpect(status().isBadRequest());

                verifyNoInteractions(medicalEventService);
        }

        @Test
        void shouldRejectMissingEventDate() throws Exception {
                mockMvc.perform(post(BASE_URL)
                                .with(user("mario@test.com"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                    "serviceId": 1
                                                }
                                                """))
                                .andExpect(status().isBadRequest());

                verifyNoInteractions(medicalEventService);
        }

        @Test
        void shouldRejectInvalidEventDate() throws Exception {
                mockMvc.perform(post(BASE_URL)
                                .with(user("mario@test.com"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                    "serviceId": 1,
                                                    "eventDate": "not-a-date"
                                                }
                                                """))
                                .andExpect(status().isBadRequest());

                verifyNoInteractions(medicalEventService);
        }

        @Test
        void shouldRejectMalformedJson() throws Exception {
                mockMvc.perform(post(BASE_URL)
                                .with(user("mario@test.com"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{ invalid json }"))
                                .andExpect(status().isBadRequest());

                verifyNoInteractions(medicalEventService);
        }

        @Test
        void shouldReturnNotFoundForMissingMedicalService() throws Exception {
                when(medicalEventService.createEvent(
                                eq("mario@test.com"), any()))
                                .thenThrow(new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "Medical service not found"));

                mockMvc.perform(post(BASE_URL)
                                .with(user("mario@test.com"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(VALID_REQUEST))
                                .andExpect(status().isNotFound());
        }

        @Test
        void shouldRejectArchivedMedicalService() throws Exception {
                when(medicalEventService.createEvent(
                                eq("mario@test.com"), any()))
                                .thenThrow(new ResponseStatusException(
                                                HttpStatus.BAD_REQUEST,
                                                "Medical service is archived"));

                mockMvc.perform(post(BASE_URL)
                                .with(user("mario@test.com"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(VALID_REQUEST))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void shouldRejectUnsupportedDiagnosticOptions() throws Exception {
                when(medicalEventService.createEvent(
                                eq("mario@test.com"), any()))
                                .thenThrow(new ResponseStatusException(
                                                HttpStatus.BAD_REQUEST,
                                                "Diagnostic options are not supported yet"));

                mockMvc.perform(post(BASE_URL)
                                .with(user("mario@test.com"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                    "serviceId": 1,
                                                    "eventDate": "2026-11-20",
                                                    "optionIds": [5]
                                                }
                                                """))
                                .andExpect(status().isBadRequest());
        }
}
