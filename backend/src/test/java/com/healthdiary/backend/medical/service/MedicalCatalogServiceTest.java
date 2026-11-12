package com.healthdiary.backend.medical.service;

import com.healthdiary.backend.auth.repository.UserRepository;
import com.healthdiary.backend.medical.repository.MedicalServiceRepository;
import com.healthdiary.backend.medical.repository.MedicalServiceOptionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.when;
import java.util.List;
import com.healthdiary.backend.auth.entity.User;
import com.healthdiary.backend.medical.dto.CreateMedicalServiceRequest;
import com.healthdiary.backend.medical.dto.UpdateMedicalServiceRequest;
import com.healthdiary.backend.medical.entity.MedicalServiceCategory;
import java.util.Optional;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import com.healthdiary.backend.medical.entity.MedicalService;
import com.healthdiary.backend.medical.dto.MedicalServiceResponse;
import org.springframework.http.HttpStatus;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MedicalCatalogServiceTest {

    @Mock
    private MedicalServiceRepository medicalServiceRepository;

    @Mock
    private MedicalServiceOptionRepository medicalServiceOptionRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private MedicalCatalogService medicalCatalogService;

    @Test
    void getDefaultServicesReturnsEmptyListWhenNoServicesExist() {

        when(medicalServiceRepository
                .findByUserIsNullAndActiveTrueOrderByNameAsc())
                .thenReturn(List.of());

        var result = medicalCatalogService.getDefaultServices();

        assertTrue(result.isEmpty());
    }

    @Test
    void createServiceRejectsDuplicateNameAndCategory() {

        User user = mock(User.class);

        when(userRepository.findByEmailIgnoreCase("mario@test.com"))
                .thenReturn(Optional.of(user));

        when(user.getId()).thenReturn(1L);

        when(medicalServiceRepository
                .existsByUserIdAndNameIgnoreCaseAndCategory(
                        1L,
                        "Visita di prova",
                        MedicalServiceCategory.SPECIALIST))
                .thenReturn(true);

        CreateMedicalServiceRequest request = new CreateMedicalServiceRequest(
                "  Visita di prova  ",
                MedicalServiceCategory.SPECIALIST);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> medicalCatalogService.createService(
                        "mario@test.com",
                        request));

        assertEquals(409, exception.getStatusCode().value());

        verify(medicalServiceRepository, never())
                .save(any());
    }

    @Test
    void updateServiceRejectsServiceOwnedByAnotherUser() {

        User user = mock(User.class);

        when(userRepository.findByEmailIgnoreCase("giulia@test.com"))
                .thenReturn(Optional.of(user));

        when(user.getId()).thenReturn(2L);

        when(medicalServiceRepository.findByIdAndUserId(153L, 2L))
                .thenReturn(Optional.empty());

        UpdateMedicalServiceRequest request = new UpdateMedicalServiceRequest(
                "Modifica non autorizzata");

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> medicalCatalogService.updateService(
                        153L,
                        "giulia@test.com",
                        request));

        assertEquals(404, exception.getStatusCode().value());
    }

    @Test
    void getDefaultServicesByCategoryReturnsEmptyList() {

        when(medicalServiceRepository
                .findByUserIsNullAndCategoryAndActiveTrueOrderByNameAsc(
                        MedicalServiceCategory.DIAGNOSTIC))
                .thenReturn(List.of());

        var result = medicalCatalogService.getDefaultServicesByCategory(
                MedicalServiceCategory.DIAGNOSTIC);

        assertTrue(result.isEmpty());
    }

    @Test
    void getOptionsReturnsEmptyListForValidService() {

        MedicalService service = mock(MedicalService.class);

        when(medicalServiceRepository
                .findByIdAndUserIsNullAndActiveTrue(76L))
                .thenReturn(Optional.of(service));

        when(medicalServiceOptionRepository
                .findByServiceIdAndActiveTrueOrderByNameAsc(76L))
                .thenReturn(List.of());

        var result = medicalCatalogService.getOptions(76L);

        assertTrue(result.isEmpty());
    }

    @Test
    void getOptionsRejectsNonexistentService() {

        when(medicalServiceRepository
                .findByIdAndUserIsNullAndActiveTrue(999L))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> medicalCatalogService.getOptions(999L));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());

        verify(medicalServiceOptionRepository, never())
                .findByServiceIdAndActiveTrueOrderByNameAsc(anyLong());
    }

    @Test
    void getUserServicesReturnsEmptyList() {

        User user = mock(User.class);

        when(userRepository.findByEmailIgnoreCase("mario@test.com"))
                .thenReturn(Optional.of(user));

        when(user.getId()).thenReturn(1L);

        when(medicalServiceRepository
                .findByUserIdAndActiveTrueOrderByNameAsc(1L))
                .thenReturn(List.of());

        var result = medicalCatalogService.getUserServices(
                "mario@test.com");

        assertTrue(result.isEmpty());
    }

    @Test
    void getArchivedServicesReturnsEmptyList() {

        User user = mock(User.class);

        when(userRepository.findByEmailIgnoreCase("mario@test.com"))
                .thenReturn(Optional.of(user));

        when(user.getId()).thenReturn(1L);

        when(medicalServiceRepository
                .findByUserIdAndActiveFalseOrderByNameAsc(1L))
                .thenReturn(List.of());

        var result = medicalCatalogService.getArchivedServices(
                "mario@test.com");

        assertTrue(result.isEmpty());
    }

    @Test
    void createServiceRejectsUnknownUser() {

        when(userRepository.findByEmailIgnoreCase("unknown@test.com"))
                .thenReturn(Optional.empty());

        CreateMedicalServiceRequest request = new CreateMedicalServiceRequest(
                "Visita personalizzata",
                MedicalServiceCategory.SPECIALIST);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> medicalCatalogService.createService(
                        "unknown@test.com",
                        request));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());

        verify(medicalServiceRepository, never()).save(any());
    }

    @Test
    void updateServiceRejectsNonexistentService() {

        User user = mock(User.class);

        when(userRepository.findByEmailIgnoreCase("mario@test.com"))
                .thenReturn(Optional.of(user));

        when(user.getId()).thenReturn(1L);

        when(medicalServiceRepository.findByIdAndUserId(999L, 1L))
                .thenReturn(Optional.empty());

        UpdateMedicalServiceRequest request = new UpdateMedicalServiceRequest("Nuovo nome");

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> medicalCatalogService.updateService(
                        999L,
                        "mario@test.com",
                        request));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void updateServiceRejectsDuplicateName() {

        User user = mock(User.class);

        MedicalService service = mock(MedicalService.class);

        when(userRepository.findByEmailIgnoreCase("mario@test.com"))
                .thenReturn(Optional.of(user));

        when(user.getId()).thenReturn(1L);

        when(medicalServiceRepository.findByIdAndUserId(153L, 1L))
                .thenReturn(Optional.of(service));

        when(service.getName()).thenReturn("Visita originale");

        when(service.getCategory())
                .thenReturn(MedicalServiceCategory.SPECIALIST);

        when(medicalServiceRepository
                .existsByUserIdAndNameIgnoreCaseAndCategory(
                        1L,
                        "Visita duplicata",
                        MedicalServiceCategory.SPECIALIST))
                .thenReturn(true);

        UpdateMedicalServiceRequest request = new UpdateMedicalServiceRequest("Visita duplicata");

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> medicalCatalogService.updateService(
                        153L,
                        "mario@test.com",
                        request));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());

        verify(service, never()).rename(anyString());
    }

    @Test
    void archiveServiceRejectsNonexistentService() {

        User user = mock(User.class);

        when(userRepository.findByEmailIgnoreCase("mario@test.com"))
                .thenReturn(Optional.of(user));

        when(user.getId()).thenReturn(1L);

        when(medicalServiceRepository.findByIdAndUserId(999L, 1L))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> medicalCatalogService.archiveService(
                        999L,
                        "mario@test.com"));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void restoreServiceRejectsNonexistentService() {

        User user = mock(User.class);

        when(userRepository.findByEmailIgnoreCase("mario@test.com"))
                .thenReturn(Optional.of(user));

        when(user.getId()).thenReturn(1L);

        when(medicalServiceRepository.findByIdAndUserId(999L, 1L))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> medicalCatalogService.restoreService(
                        999L,
                        "mario@test.com"));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void archiveServiceCallsArchive() {

        User user = mock(User.class);
        MedicalService service = mock(MedicalService.class);

        when(userRepository.findByEmailIgnoreCase("mario@test.com"))
                .thenReturn(Optional.of(user));

        when(user.getId()).thenReturn(1L);

        when(medicalServiceRepository.findByIdAndUserId(153L, 1L))
                .thenReturn(Optional.of(service));

        medicalCatalogService.archiveService(153L, "mario@test.com");

        verify(service).archive();
    }

    @Test
    void restoreServiceCallsRestore() {

        User user = mock(User.class);
        MedicalService service = mock(MedicalService.class);

        when(userRepository.findByEmailIgnoreCase("mario@test.com"))
                .thenReturn(Optional.of(user));

        when(user.getId()).thenReturn(1L);

        when(medicalServiceRepository.findByIdAndUserId(153L, 1L))
                .thenReturn(Optional.of(service));

        medicalCatalogService.restoreService(153L, "mario@test.com");

        verify(service).restore();
    }

    @Test
    void updateServiceRenamesService() {

        User user = mock(User.class);
        MedicalService service = mock(MedicalService.class);

        when(userRepository.findByEmailIgnoreCase("mario@test.com"))
                .thenReturn(Optional.of(user));

        when(user.getId()).thenReturn(1L);

        when(medicalServiceRepository.findByIdAndUserId(153L, 1L))
                .thenReturn(Optional.of(service));

        when(service.getName()).thenReturn("Vecchio nome");

        when(service.getCategory())
                .thenReturn(MedicalServiceCategory.SPECIALIST);

        UpdateMedicalServiceRequest request = new UpdateMedicalServiceRequest("  Nuovo nome  ");

        medicalCatalogService.updateService(
                153L,
                "mario@test.com",
                request);

        verify(service).rename("Nuovo nome");
    }

    @Test
    void createServiceSuccessfully() {

        User user = mock(User.class);

        when(userRepository.findByEmailIgnoreCase("mario@test.com"))
                .thenReturn(Optional.of(user));

        when(user.getId()).thenReturn(1L);

        when(medicalServiceRepository.save(any(MedicalService.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CreateMedicalServiceRequest request = new CreateMedicalServiceRequest(
                "  Visita personalizzata  ",
                MedicalServiceCategory.SPECIALIST);

        MedicalServiceResponse response = medicalCatalogService.createService("mario@test.com", request);

        assertEquals("Visita personalizzata", response.name());
        assertEquals(MedicalServiceCategory.SPECIALIST, response.category());

        verify(medicalServiceRepository).save(any(MedicalService.class));
    }
}