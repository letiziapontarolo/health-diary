package com.healthdiary.backend.medical.service;

import com.healthdiary.backend.auth.entity.User;
import com.healthdiary.backend.auth.repository.UserRepository;
import com.healthdiary.backend.medical.dto.CreateExamResultRequest;
import com.healthdiary.backend.medical.entity.*;
import com.healthdiary.backend.medical.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExamResultServiceTest {
    @Mock
    UserRepository users;
    @Mock
    MedicalEventRepository events;
    @Mock
    LabParameterRepository parameters;
    @Mock
    MeasurementUnitRepository units;
    @Mock
    ExamResultRepository results;
    @Mock
    ExamResultValidator validator;
    @Mock
    ExamResultRangeCalculator calculator;
    @InjectMocks
    ExamResultService service;
    User user;
    MedicalEvent exam;
    LabParameter parameter;

    @BeforeEach
    void setup() {
        user = mock(User.class);
        exam = mock(MedicalEvent.class);
        var medicalService = new MedicalService(null, "Analisi del sangue", MedicalServiceCategory.LABORATORY, null);
        ReflectionTestUtils.setField(medicalService, "id", 2L);
        parameter = new LabParameter(medicalService, null, "TSH", null);
        ReflectionTestUtils.setField(parameter, "id", 3L);
    }

    private void owned() {
        when(users.findByEmailIgnoreCase("u@test.it")).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(7L);
        when(events.findByIdAndUserId(10L, 7L)).thenReturn(Optional.of(exam));
        when(exam.getService()).thenReturn(parameter.getService());
    }

    private CreateExamResultRequest req(int position) {
        return new CreateExamResultRequest(3L, position, ExamResultValueType.NUMERIC, new BigDecimal("2.5"), null, null,
                ExamResultIntervalType.RANGE, new BigDecimal("0.4"), new BigDecimal("4"), true, true, null);
    }

    @Test
    void cannotListAnotherUsersExam() {
        when(users.findByEmailIgnoreCase("u@test.it")).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(7L);
        when(events.findByIdAndUserId(10L, 7L)).thenReturn(Optional.empty());
        var e = assertThrows(ResponseStatusException.class, () -> service.list(10L, "u@test.it"));
        assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
        verifyNoInteractions(results);
    }

    @Test
    void listReturnsEmptyForOwnedExam() {
        owned();
        when(results.findByExamIdOrderByPositionAsc(10L)).thenReturn(List.of());
        assertTrue(service.list(10L, "u@test.it").isEmpty());
    }

    @Test
    void createRejectsOccupiedPosition() {
        owned();
        when(results.existsByExamIdAndPosition(10L, 1)).thenReturn(true);
        var e = assertThrows(ResponseStatusException.class, () -> service.create(10L, "u@test.it", req(1)));
        assertEquals(HttpStatus.CONFLICT, e.getStatusCode());
        verify(results, never()).save(any());
    }

    @Test
    void createRejectsParameterFromOtherService() {
        owned();
        var otherService = new MedicalService(null, "Urine", MedicalServiceCategory.LABORATORY, null);
        ReflectionTestUtils.setField(otherService, "id", 4L);
        when(parameters.findById(3L)).thenReturn(Optional.of(new LabParameter(otherService, null, "TSH", null)));
        var e = assertThrows(ResponseStatusException.class, () -> service.create(10L, "u@test.it", req(1)));
        assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
        verify(results, never()).save(any());
    }

    @Test
    void deleteRejectsMissingResult() {
        owned();
        when(results.findByIdAndExamId(99L, 10L)).thenReturn(Optional.empty());
        assertEquals(HttpStatus.NOT_FOUND,
                assertThrows(ResponseStatusException.class, () -> service.delete(10L, 99L, "u@test.it"))
                        .getStatusCode());
    }

    @Test
    void deleteOwnedResultRemovesIt() {
        owned();
        ExamResult result = mock(ExamResult.class);
        when(results.findByIdAndExamId(99L, 10L)).thenReturn(Optional.of(result));
        service.delete(10L, 99L, "u@test.it");
        verify(results).delete(result);
    }

    @Test
    void createPersistsValidResultAndReturnsResponse() {
        owned();
        when(exam.getId()).thenReturn(10L);
        when(parameters.findById(3L)).thenReturn(Optional.of(parameter));
        when(results.save(any(ExamResult.class))).thenAnswer(i -> i.getArgument(0));
        var created = service.create(10L, "u@test.it", req(1));
        assertEquals(3L, created.parameterId());
        assertEquals(1, created.position());
        assertEquals(new BigDecimal("2.5"), created.numericValue());
        verify(results).save(any(ExamResult.class));
    }

    @Test
    void updateRejectsMissingResult() {
        owned();
        when(results.findByIdAndExamId(99L, 10L)).thenReturn(Optional.empty());
        var e = assertThrows(ResponseStatusException.class, () -> service.update(10L, 99L, "u@test.it", updateReq(1)));
        assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
    }

    @Test
    void updateRejectsDuplicateNewPosition() {
        owned();
        ExamResult existing = mock(ExamResult.class);
        when(existing.getPosition()).thenReturn(1);
        when(results.findByIdAndExamId(99L, 10L)).thenReturn(Optional.of(existing));
        when(results.existsByExamIdAndPosition(10L, 2)).thenReturn(true);
        assertEquals(HttpStatus.CONFLICT, assertThrows(ResponseStatusException.class,
                () -> service.update(10L, 99L, "u@test.it", updateReq(2))).getStatusCode());
        verify(existing, never()).update(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                any());
    }

    @Test
    void updateExistingResultChangesValue() {
        owned();
        when(exam.getId()).thenReturn(10L);
        ExamResult existing = new ExamResult(exam, parameter, 1, ExamResultValueType.NUMERIC,
                new BigDecimal("1.0"), null, null, ExamResultIntervalType.RANGE,
                new BigDecimal("0.4"), new BigDecimal("4"), true, true, null);
        ReflectionTestUtils.setField(existing, "id", 99L);
        when(results.findByIdAndExamId(99L, 10L)).thenReturn(Optional.of(existing));
        when(parameters.findById(3L)).thenReturn(Optional.of(parameter));
        var updated = service.update(10L, 99L, "u@test.it", updateReq(2));
        assertEquals(2, updated.position());
        assertEquals(new BigDecimal("2.5"), updated.numericValue());
        verify(results, never()).save(any());
    }

    @Test
    void trendRejectsAnotherUsersCustomParameter() {
        when(users.findByEmailIgnoreCase("u@test.it")).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(7L);
        User other = mock(User.class);
        when(other.getId()).thenReturn(8L);
        LabParameter custom = new LabParameter(parameter.getService(), other, "Private", null);
        when(parameters.findById(3L)).thenReturn(Optional.of(custom));
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(ResponseStatusException.class,
                () -> service.trend(3L, "u@test.it")).getStatusCode());
        verifyNoInteractions(results);
    }

    @Test
    void trendReturnsEmptyWhenNoMeasurements() {
        when(users.findByEmailIgnoreCase("u@test.it")).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(7L);
        when(parameters.findById(3L)).thenReturn(Optional.of(parameter));
        assertTrue(service.trend(3L, "u@test.it").isEmpty());
        verify(results).findByParameterIdAndExamUserIdOrderByExamEventDateAsc(3L, 7L);
    }

    @Test
    void createRejectsUnknownUnit() {
        owned();
        when(parameters.findById(3L)).thenReturn(Optional.of(parameter));
        var r = new CreateExamResultRequest(3L, 1, ExamResultValueType.NUMERIC,
                new BigDecimal("2.5"), null, 99L, ExamResultIntervalType.RANGE,
                new BigDecimal("0.4"), new BigDecimal("4"), true, true, null);
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
                () -> service.create(10L, "u@test.it", r)).getStatusCode());
        verify(results, never()).save(any());
    }

    private com.healthdiary.backend.medical.dto.UpdateExamResultRequest updateReq(int position) {
        return new com.healthdiary.backend.medical.dto.UpdateExamResultRequest(3L, position,
                ExamResultValueType.NUMERIC, new BigDecimal("2.5"), null, null, ExamResultIntervalType.RANGE,
                new BigDecimal("0.4"), new BigDecimal("4"), true, true, null);
    }

}