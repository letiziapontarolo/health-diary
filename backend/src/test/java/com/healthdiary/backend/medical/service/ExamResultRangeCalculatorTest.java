package com.healthdiary.backend.medical.service;

import com.healthdiary.backend.medical.entity.ExamResultIntervalType;
import com.healthdiary.backend.medical.entity.ExamResultValueType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class ExamResultRangeCalculatorTest {

    private final ExamResultRangeCalculator calculator =
            new ExamResultRangeCalculator();

    private Boolean calculate(
            String value,
            ExamResultIntervalType intervalType,
            String lower,
            String upper,
            Boolean lowerInclusive,
            Boolean upperInclusive) {

        return calculator.isOutOfRange(
                ExamResultValueType.NUMERIC,
                new BigDecimal(value),
                intervalType,
                lower == null ? null : new BigDecimal(lower),
                upper == null ? null : new BigDecimal(upper),
                lowerInclusive,
                upperInclusive);
    }

    @Test
    void valueInsideRangeIsNormal() {
        assertEquals(false, calculate(
                "2.50", ExamResultIntervalType.RANGE,
                "0.40", "4.00", true, true));
    }

    @Test
    void valueBelowRangeIsOutOfRange() {
        assertEquals(true, calculate(
                "0.20", ExamResultIntervalType.RANGE,
                "0.40", "4.00", true, true));
    }

    @Test
    void valueAboveRangeIsOutOfRange() {
        assertEquals(true, calculate(
                "5.20", ExamResultIntervalType.RANGE,
                "0.40", "4.00", true, true));
    }

    @Test
    void inclusiveBoundaryIsNormal() {
        assertEquals(false, calculate(
                "0.40", ExamResultIntervalType.RANGE,
                "0.40", "4.00", true, true));
    }

    @Test
    void exclusiveBoundaryIsOutOfRange() {
        assertEquals(true, calculate(
                "0.40", ExamResultIntervalType.RANGE,
                "0.40", "4.00", false, true));
    }

    @Test
    void lowerBoundDetectsLowValue() {
        assertEquals(true, calculate(
                "9", ExamResultIntervalType.LOWER_BOUND,
                "10", null, true, null));
    }

    @Test
    void upperBoundDetectsHighValue() {
        assertEquals(true, calculate(
                "101", ExamResultIntervalType.UPPER_BOUND,
                null, "100", null, true));
    }

    @Test
    void textIntervalCannotBeComparedNumerically() {
        assertNull(calculate(
                "2.50", ExamResultIntervalType.TEXT,
                null, null, null, null));
    }

    @Test
    void unavailableResultHasNoNumericComparison() {
        assertNull(calculator.isOutOfRange(
                ExamResultValueType.UNAVAILABLE,
                null,
                ExamResultIntervalType.RANGE,
                BigDecimal.ZERO,
                BigDecimal.TEN,
                true,
                true));
    }

    @Test
    void resultWithoutIntervalHasNoComparison() {
        assertNull(calculate(
                "2.50", null,
                null, null, null, null));
    }
}
