package com.healthdiary.backend.medical.service;

import com.healthdiary.backend.medical.dto.CreateExamResultRequest;
import com.healthdiary.backend.medical.entity.ExamResultIntervalType;
import com.healthdiary.backend.medical.entity.ExamResultValueType;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class ExamResultValidatorTest {

    private final ExamResultValidator validator = new ExamResultValidator();

    private CreateExamResultRequest request(
            ExamResultValueType valueType,
            BigDecimal numericValue,
            String textValue,
            ExamResultIntervalType intervalType,
            BigDecimal lower,
            BigDecimal upper,
            String textInterval) {

        return new CreateExamResultRequest(
                1L, 1, valueType, numericValue, textValue,
                null, intervalType, lower, upper,
                intervalType == ExamResultIntervalType.RANGE || intervalType == ExamResultIntervalType.LOWER_BOUND ? true : null,
                intervalType == ExamResultIntervalType.RANGE || intervalType == ExamResultIntervalType.UPPER_BOUND ? true : null,
                textInterval);
    }

    @Test
    void acceptsValidNumericResult() {
        var result = request(
                ExamResultValueType.NUMERIC,
                new BigDecimal("2.50"),
                null,
                ExamResultIntervalType.RANGE,
                new BigDecimal("0.40"),
                new BigDecimal("4.00"),
                null);

        assertDoesNotThrow(() -> validator.validate(result));
    }

    @Test
    void rejectsNumericResultWithoutValue() {
        var result = request(
                ExamResultValueType.NUMERIC,
                null, null, null, null, null, null);

        assertBadRequest(result);
    }

    @Test
    void acceptsValidTextResult() {
        var result = request(
                ExamResultValueType.TEXT,
                null, "Negativo",
                null, null, null, null);

        assertDoesNotThrow(() -> validator.validate(result));
    }

    @Test
    void rejectsBlankTextResult() {
        var result = request(
                ExamResultValueType.TEXT,
                null, "   ",
                null, null, null, null);

        assertBadRequest(result);
    }

    @Test
    void acceptsUnavailableResult() {
        var result = request(
                ExamResultValueType.UNAVAILABLE,
                null, null, null, null, null, null);

        assertDoesNotThrow(() -> validator.validate(result));
    }

    @Test
    void rejectsUnavailableResultWithNumericValue() {
        var result = request(
                ExamResultValueType.UNAVAILABLE,
                BigDecimal.ONE, null,
                null, null, null, null);

        assertBadRequest(result);
    }

    @Test
    void rejectsInvertedRange() {
        var result = request(
                ExamResultValueType.NUMERIC,
                BigDecimal.ONE, null,
                ExamResultIntervalType.RANGE,
                new BigDecimal("10"),
                new BigDecimal("5"),
                null);

        assertBadRequest(result);
    }

    @Test
    void acceptsTextReferenceInterval() {
        var result = request(
                ExamResultValueType.TEXT,
                null, "Negativo",
                ExamResultIntervalType.TEXT,
                null, null, "Negativo");

        assertDoesNotThrow(() -> validator.validate(result));
    }

    private void assertBadRequest(CreateExamResultRequest request) {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> validator.validate(request));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }
}
