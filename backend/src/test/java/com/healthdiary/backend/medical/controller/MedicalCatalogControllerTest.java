package com.healthdiary.backend.medical.controller;

import com.healthdiary.backend.config.SecurityConfig;
import com.healthdiary.backend.medical.dto.MedicalServiceOptionResponse;
import com.healthdiary.backend.medical.dto.MedicalServiceResponse;
import com.healthdiary.backend.medical.entity.MedicalServiceCategory;
import com.healthdiary.backend.medical.entity.OptionSelectionMode;
import com.healthdiary.backend.medical.service.MedicalCatalogService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MedicalCatalogController.class)
@Import(SecurityConfig.class)
class MedicalCatalogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MedicalCatalogService medicalCatalogService;

    private MedicalServiceResponse sampleService() {
        return new MedicalServiceResponse(
                153L,
                "Visita personalizzata",
                MedicalServiceCategory.SPECIALIST,
                null,
                OptionSelectionMode.MULTIPLE
        );
    }

    @Test
    void shouldRejectUnauthenticatedRequests() throws Exception {
        mockMvc.perform(get("/api/medical-services"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/medical-services/mine"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnDefaultServices() throws Exception {
        when(medicalCatalogService.getDefaultServices())
                .thenReturn(List.of(sampleService()));

        mockMvc.perform(get("/api/medical-services")
                .with(user("mario@test.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(153))
                .andExpect(jsonPath("$[0].name").value("Visita personalizzata"))
                .andExpect(jsonPath("$[0].category").value("SPECIALIST"));
    }

    @Test
    void shouldFilterDefaultServicesByCategory() throws Exception {
        when(medicalCatalogService.getDefaultServicesByCategory(
                MedicalServiceCategory.DIAGNOSTIC))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/medical-services")
                .param("category", "DIAGNOSTIC")
                .with(user("mario@test.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(medicalCatalogService)
                .getDefaultServicesByCategory(MedicalServiceCategory.DIAGNOSTIC);
    }

    @Test
    void shouldRejectInvalidCategory() throws Exception {
        mockMvc.perform(get("/api/medical-services")
                .param("category", "INVALID")
                .with(user("mario@test.com")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnServiceOptions() throws Exception {
        when(medicalCatalogService.getOptions(76L))
                .thenReturn(List.of(
                        new MedicalServiceOptionResponse(1L, "Opzione A")
                ));

        mockMvc.perform(get("/api/medical-services/76/options")
                .with(user("mario@test.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Opzione A"));
    }

    @Test
    void shouldReturnNotFoundForMissingServiceOptions() throws Exception {
        when(medicalCatalogService.getOptions(999L))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));

        mockMvc.perform(get("/api/medical-services/999/options")
                .with(user("mario@test.com")))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnPersonalServices() throws Exception {
        when(medicalCatalogService.getUserServices("mario@test.com"))
                .thenReturn(List.of(sampleService()));

        mockMvc.perform(get("/api/medical-services/mine")
                .with(user("mario@test.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(153));
    }

    @Test
    void shouldReturnArchivedServices() throws Exception {
        when(medicalCatalogService.getArchivedServices("mario@test.com"))
                .thenReturn(List.of(sampleService()));

        mockMvc.perform(get("/api/medical-services/mine/archived")
                .with(user("mario@test.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Visita personalizzata"));
    }

    @Test
    void shouldCreatePersonalService() throws Exception {
        when(medicalCatalogService.createService(eq("mario@test.com"), any()))
                .thenReturn(sampleService());

        mockMvc.perform(post("/api/medical-services")
                .with(user("mario@test.com"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "name": "Visita personalizzata",
                          "category": "SPECIALIST"
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(153))
                .andExpect(jsonPath("$.name").value("Visita personalizzata"));
    }

    @Test
    void shouldRejectDuplicatePersonalService() throws Exception {
        when(medicalCatalogService.createService(eq("mario@test.com"), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT));

        mockMvc.perform(post("/api/medical-services")
                .with(user("mario@test.com"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "name": "Visita personalizzata",
                          "category": "SPECIALIST"
                        }
                        """))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldRejectInvalidServiceName() throws Exception {
        mockMvc.perform(post("/api/medical-services")
                .with(user("mario@test.com"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "name": "",
                          "category": "SPECIALIST"
                        }
                        """))
                .andExpect(status().isBadRequest());

        verify(medicalCatalogService, never())
                .createService(anyString(), any());
    }

    @Test
    void shouldRejectMissingCategory() throws Exception {
        mockMvc.perform(post("/api/medical-services")
                .with(user("mario@test.com"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "name": "Visita personalizzata"
                        }
                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectMissingCsrfToken() throws Exception {
        mockMvc.perform(post("/api/medical-services")
                .with(user("mario@test.com"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "name": "Visita personalizzata",
                          "category": "SPECIALIST"
                        }
                        """))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldUpdatePersonalService() throws Exception {
        when(medicalCatalogService.updateService(
                eq(153L), eq("mario@test.com"), any()))
                .thenReturn(sampleService());

        mockMvc.perform(patch("/api/medical-services/153")
                .with(user("mario@test.com"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "name": "Nuovo nome"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(153));
    }

    @Test
    void shouldRejectUpdatingAnotherUsersService() throws Exception {
        when(medicalCatalogService.updateService(
                eq(153L), eq("giulia@test.com"), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));

        mockMvc.perform(patch("/api/medical-services/153")
                .with(user("giulia@test.com"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "name": "Modifica non autorizzata"
                        }
                        """))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldArchivePersonalService() throws Exception {
        mockMvc.perform(patch("/api/medical-services/153/archive")
                .with(user("mario@test.com"))
                .with(csrf()))
                .andExpect(status().isNoContent());

        verify(medicalCatalogService)
                .archiveService(153L, "mario@test.com");
    }

    @Test
    void shouldRestorePersonalService() throws Exception {
        mockMvc.perform(patch("/api/medical-services/153/restore")
                .with(user("mario@test.com"))
                .with(csrf()))
                .andExpect(status().isNoContent());

        verify(medicalCatalogService)
                .restoreService(153L, "mario@test.com");
    }

    @Test
    void shouldRejectArchivingAnotherUsersService() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND))
                .when(medicalCatalogService)
                .archiveService(153L, "giulia@test.com");

        mockMvc.perform(patch("/api/medical-services/153/archive")
                .with(user("giulia@test.com"))
                .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectRestoringAnotherUsersService() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND))
                .when(medicalCatalogService)
                .restoreService(153L, "giulia@test.com");

        mockMvc.perform(patch("/api/medical-services/153/restore")
                .with(user("giulia@test.com"))
                .with(csrf()))
                .andExpect(status().isNotFound());
    }
}
