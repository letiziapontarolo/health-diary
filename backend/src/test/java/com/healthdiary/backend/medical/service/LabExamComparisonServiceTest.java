package com.healthdiary.backend.medical.service;

import com.healthdiary.backend.auth.entity.User;
import com.healthdiary.backend.auth.repository.UserRepository;
import com.healthdiary.backend.medical.entity.*;
import com.healthdiary.backend.medical.repository.MedicalEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LabExamComparisonServiceTest {
    @Mock
    UserRepository users;
    @Mock
    MedicalEventRepository events;
    @Mock
    ExamResultService resultService;
    @InjectMocks
    LabExamComparisonService service;

    @Test
    void rejectsComparingSameExam() {
        assertEquals(HttpStatus.BAD_REQUEST,
                assertThrows(ResponseStatusException.class, () -> service.compare(1L, 1L, "u@test.it"))
                        .getStatusCode());
        verifyNoInteractions(users, events, resultService);
    }

    @Test
    void deniesForeignFirstExam() {
        User user = mock(User.class);
        when(users.findByEmailIgnoreCase("u@test.it")).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(7L);
        assertEquals(HttpStatus.NOT_FOUND,
                assertThrows(ResponseStatusException.class, () -> service.compare(1L, 2L, "u@test.it"))
                        .getStatusCode());
        verifyNoInteractions(resultService);
    }

    @Test
    void rejectsDifferentLaboratoryServices() {
        User user = mock(User.class);
        when(users.findByEmailIgnoreCase("u@test.it")).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(7L);
        MedicalEvent first = mock(MedicalEvent.class), second = mock(MedicalEvent.class);
        MedicalService a = new MedicalService(null, "Sangue", MedicalServiceCategory.LABORATORY, null);
        MedicalService b = new MedicalService(null, "Urine", MedicalServiceCategory.LABORATORY, null);
        org.springframework.test.util.ReflectionTestUtils.setField(a, "id", 11L);
        org.springframework.test.util.ReflectionTestUtils.setField(b, "id", 12L);
        when(events.findByIdAndUserId(1L, 7L)).thenReturn(Optional.of(first));
        when(events.findByIdAndUserId(2L, 7L)).thenReturn(Optional.of(second));
        when(first.getService()).thenReturn(a);
        when(second.getService()).thenReturn(b);
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
                () -> service.compare(1L, 2L, "u@test.it")).getStatusCode());
        verifyNoInteractions(resultService);
    }

    private MedicalEvent laboratoryExam(Long serviceId) {
        MedicalEvent event = mock(MedicalEvent.class);
        MedicalService service = new MedicalService(null, "Sangue", MedicalServiceCategory.LABORATORY, null);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "id", serviceId);
        when(event.getService()).thenReturn(service);
        return event;
    }

    private void ownedPair(MedicalEvent first, MedicalEvent second) {
        User user = mock(User.class);
        when(users.findByEmailIgnoreCase("u@test.it")).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(7L);
        when(events.findByIdAndUserId(1L, 7L)).thenReturn(Optional.of(first));
        when(events.findByIdAndUserId(2L, 7L)).thenReturn(Optional.of(second));
    }

    private com.healthdiary.backend.medical.dto.ExamResultResponse result(Long parameterId, String name,
            Integer position, Long unitId) {
        return new com.healthdiary.backend.medical.dto.ExamResultResponse(null, null, parameterId, name, position,
                ExamResultValueType.NUMERIC, java.math.BigDecimal.ONE, null, unitId, null, null, null, null, null, null,
                null, null);
    }

    @Test
    void comparesEmptyExams() {
        MedicalEvent first = laboratoryExam(10L), second = laboratoryExam(10L);
        ownedPair(first, second);
        when(resultService.list(1L, "u@test.it")).thenReturn(java.util.List.of());
        when(resultService.list(2L, "u@test.it")).thenReturn(java.util.List.of());
        assertTrue(service.compare(1L, 2L, "u@test.it").results().isEmpty());
    }

    @Test
    void comparisonKeepsUnmatchedParameters() {
        MedicalEvent first = laboratoryExam(10L), second = laboratoryExam(10L);
        ownedPair(first, second);
        when(resultService.list(1L, "u@test.it")).thenReturn(java.util.List.of(result(3L, "TSH", 1, 1L)));
        when(resultService.list(2L, "u@test.it")).thenReturn(java.util.List.of(result(4L, "FT4", 1, 1L)));
        var compared = service.compare(1L, 2L, "u@test.it").results();
        assertEquals(2, compared.size());
        assertNull(compared.get(0).second());
        assertNull(compared.get(1).first());
    }

    @Test
    void comparisonDetectsDifferentUnits() {
        MedicalEvent first = laboratoryExam(10L), second = laboratoryExam(10L);
        ownedPair(first, second);
        when(resultService.list(1L, "u@test.it")).thenReturn(java.util.List.of(result(3L, "TSH", 1, 1L)));
        when(resultService.list(2L, "u@test.it")).thenReturn(java.util.List.of(result(3L, "TSH", 1, 2L)));
        var compared = service.compare(1L, 2L, "u@test.it").results();
        assertEquals(1, compared.size());
        assertFalse(compared.get(0).unitsMatch());
    }

    @Test
    void comparisonMatchesRepeatedParameterByOccurrence() {
        MedicalEvent first = laboratoryExam(10L), second = laboratoryExam(10L);
        ownedPair(first, second);
        when(resultService.list(1L, "u@test.it"))
                .thenReturn(java.util.List.of(result(3L, "TSH", 1, 1L), result(3L, "TSH", 2, 1L)));
        when(resultService.list(2L, "u@test.it")).thenReturn(java.util.List.of(result(3L, "TSH", 1, 1L)));
        var compared = service.compare(1L, 2L, "u@test.it").results();
        assertEquals(2, compared.size());
        assertEquals(2, compared.get(1).occurrence());
        assertNull(compared.get(1).second());
        assertTrue(compared.get(0).unitsMatch());
    }

}