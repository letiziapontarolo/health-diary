package com.healthdiary.backend.medical.service;

import com.healthdiary.backend.auth.entity.User;
import com.healthdiary.backend.auth.repository.UserRepository;
import com.healthdiary.backend.medical.entity.*;
import com.healthdiary.backend.medical.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.healthdiary.backend.medical.dto.CreateLabParameterRequest;
import com.healthdiary.backend.medical.dto.UpdateLabParameterRequest;
import org.mockito.ArgumentCaptor;
import java.util.List;
import java.util.Optional;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LabParameterCatalogServiceTest {

    @Mock
    private LabParameterRepository labParameterRepository;

    @Mock
    private MedicalServiceRepository medicalServiceRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private LabParameterCatalogService service;

    private MedicalService laboratoryService;

    @BeforeEach
    void setUp() {
        laboratoryService = new MedicalService(
                null,
                "Analisi del sangue",
                MedicalServiceCategory.LABORATORY,
                null);

        ReflectionTestUtils.setField(laboratoryService, "id", 1L);
    }

    @Test
    void getDefaultParametersReturnsGlobalParameters() {
        LabParameter parameter = new LabParameter(
                laboratoryService, null, "TSH", null);

        when(medicalServiceRepository
                .findByIdAndUserIsNullAndActiveTrue(1L))
                .thenReturn(Optional.of(laboratoryService));

        when(labParameterRepository
                .findByServiceIdAndUserIsNullAndActiveTrueOrderByNameAsc(1L))
                .thenReturn(List.of(parameter));

        var result = service.getDefaultParameters(1L);

        assertEquals(1, result.size());
        assertEquals("TSH", result.get(0).name());
        assertFalse(result.get(0).custom());
    }

    @Test
    void getDefaultParametersRejectsNonLaboratoryService() {
        MedicalService diagnosticService = new MedicalService(
                null,
                "Ecografia",
                MedicalServiceCategory.DIAGNOSTIC,
                null);

        when(medicalServiceRepository
                .findByIdAndUserIsNullAndActiveTrue(1L))
                .thenReturn(Optional.of(diagnosticService));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.getDefaultParameters(1L));

        assertEquals(HttpStatus.BAD_REQUEST,
                exception.getStatusCode());

        verifyNoInteractions(labParameterRepository);
    }

    @Test
    void getDefaultParametersReturnsNotFoundForUnknownService() {
        when(medicalServiceRepository
                .findByIdAndUserIsNullAndActiveTrue(99L))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.getDefaultParameters(99L));

        assertEquals(HttpStatus.NOT_FOUND,
                exception.getStatusCode());
    }

    @Test
    void getUserParametersReturnsNotFoundForUnknownUser() {
        when(userRepository.findByEmailIgnoreCase("test@example.com"))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.getUserParameters(1L, "test@example.com"));

        assertEquals(HttpStatus.NOT_FOUND,
                exception.getStatusCode());

        verifyNoInteractions(
                medicalServiceRepository,
                labParameterRepository);
    }

    @Test
    void getUserParametersRejectsNonLaboratoryService() {
        User user = mock(User.class);

        MedicalService diagnosticService = new MedicalService(
                null,
                "Ecografia",
                MedicalServiceCategory.DIAGNOSTIC,
                null);

        when(userRepository.findByEmailIgnoreCase("test@example.com"))
                .thenReturn(Optional.of(user));

        when(medicalServiceRepository.findById(2L))
                .thenReturn(Optional.of(diagnosticService));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.getUserParameters(2L, "test@example.com"));

        assertEquals(HttpStatus.BAD_REQUEST,
                exception.getStatusCode());

        verifyNoInteractions(labParameterRepository);
    }

    @Test
    void createParameterSavesCustomParameter() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(5L);

        when(userRepository.findByEmailIgnoreCase("test@example.com"))
                .thenReturn(Optional.of(user));

        when(medicalServiceRepository.findById(1L))
                .thenReturn(Optional.of(laboratoryService));

        when(labParameterRepository.save(any(LabParameter.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var request = new CreateLabParameterRequest(
                1L, "  Vitamina D  ", null);

        var result = service.createParameter(
                "test@example.com", request);

        assertEquals("Vitamina D", result.name());
        assertTrue(result.custom());

        ArgumentCaptor<LabParameter> captor = ArgumentCaptor.forClass(LabParameter.class);

        verify(labParameterRepository).save(captor.capture());

        assertEquals("Vitamina D", captor.getValue().getName());
        assertSame(user, captor.getValue().getUser());
    }

    @Test
    void createParameterRejectsDuplicateName() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(5L);

        when(userRepository.findByEmailIgnoreCase("test@example.com"))
                .thenReturn(Optional.of(user));

        when(medicalServiceRepository.findById(1L))
                .thenReturn(Optional.of(laboratoryService));

        when(labParameterRepository
                .existsByServiceIdAndUserIdAndNameIgnoreCase(
                        1L, 5L, "TSH"))
                .thenReturn(true);

        var request = new CreateLabParameterRequest(
                1L, " TSH ", null);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.createParameter(
                        "test@example.com", request));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(labParameterRepository, never()).save(any());
    }

    @Test
    void updateParameterRenamesOwnedParameter() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(5L);

        LabParameter parameter = new LabParameter(
                laboratoryService, user, "Vitamina D", null);

        when(userRepository.findByEmailIgnoreCase("test@example.com"))
                .thenReturn(Optional.of(user));

        when(labParameterRepository.findByIdAndUserId(10L, 5L))
                .thenReturn(Optional.of(parameter));

        var result = service.updateParameter(
                10L,
                "test@example.com",
                new UpdateLabParameterRequest("  Vitamina D3  "));

        assertEquals("Vitamina D3", result.name());
        assertEquals("Vitamina D3", parameter.getName());
    }

    @Test
    void updateParameterRejectsParameterOwnedByAnotherUser() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(5L);

        when(userRepository.findByEmailIgnoreCase("test@example.com"))
                .thenReturn(Optional.of(user));

        when(labParameterRepository.findByIdAndUserId(10L, 5L))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.updateParameter(
                        10L,
                        "test@example.com",
                        new UpdateLabParameterRequest("TSH")));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void archiveAndRestoreParameterChangesActiveStatus() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(5L);

        LabParameter parameter = new LabParameter(
                laboratoryService, user, "Vitamina D", null);

        when(userRepository.findByEmailIgnoreCase("test@example.com"))
                .thenReturn(Optional.of(user));

        when(labParameterRepository.findByIdAndUserId(10L, 5L))
                .thenReturn(Optional.of(parameter));

        service.archiveParameter(10L, "test@example.com");
        assertFalse(parameter.isActive());

        service.restoreParameter(10L, "test@example.com");
        assertTrue(parameter.isActive());
    }

    @Test
    void createParameterRejectsServiceOwnedByAnotherUser() {
        User currentUser = mock(User.class);
        User otherUser = mock(User.class);

        when(currentUser.getId()).thenReturn(5L);
        when(otherUser.getId()).thenReturn(9L);

        MedicalService privateService = new MedicalService(
                otherUser,
                "Analisi personalizzate",
                MedicalServiceCategory.LABORATORY,
                null);

        when(userRepository.findByEmailIgnoreCase("test@example.com"))
                .thenReturn(Optional.of(currentUser));

        when(medicalServiceRepository.findById(20L))
                .thenReturn(Optional.of(privateService));

        var request = new CreateLabParameterRequest(
                20L, "TSH", null);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.createParameter(
                        "test@example.com", request));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(labParameterRepository, never()).save(any());
    }

    @Test
    void createParameterRejectsNonLaboratoryService() {
        User user = mock(User.class);

        MedicalService diagnosticService = new MedicalService(
                null,
                "Ecografia tiroidea",
                MedicalServiceCategory.DIAGNOSTIC,
                null);

        when(userRepository.findByEmailIgnoreCase("test@example.com"))
                .thenReturn(Optional.of(user));

        when(medicalServiceRepository.findById(2L))
                .thenReturn(Optional.of(diagnosticService));

        var request = new CreateLabParameterRequest(
                2L, "TSH", null);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.createParameter(
                        "test@example.com", request));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(labParameterRepository, never()).save(any());
    }

    @Test
    void updateParameterRejectsDuplicateName() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(5L);

        LabParameter parameter = new LabParameter(
                laboratoryService, user, "Vitamina D", null);

        when(userRepository.findByEmailIgnoreCase("test@example.com"))
                .thenReturn(Optional.of(user));

        when(labParameterRepository.findByIdAndUserId(10L, 5L))
                .thenReturn(Optional.of(parameter));

        when(labParameterRepository
                .existsByServiceIdAndUserIdAndNameIgnoreCase(
                        1L, 5L, "TSH"))
                .thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.updateParameter(
                        10L,
                        "test@example.com",
                        new UpdateLabParameterRequest(" TSH ")));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("Vitamina D", parameter.getName());
    }

    @Test
    void archiveParameterRejectsParameterOwnedByAnotherUser() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(5L);

        when(userRepository.findByEmailIgnoreCase("test@example.com"))
                .thenReturn(Optional.of(user));

        when(labParameterRepository.findByIdAndUserId(10L, 5L))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.archiveParameter(
                        10L, "test@example.com"));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

}
