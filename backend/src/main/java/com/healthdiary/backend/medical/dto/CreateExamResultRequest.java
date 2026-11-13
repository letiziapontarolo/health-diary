package com.healthdiary.backend.medical.dto;

import com.healthdiary.backend.medical.entity.ExamResultIntervalType;
import com.healthdiary.backend.medical.entity.ExamResultValueType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CreateExamResultRequest(
        @NotNull Long parameterId,
        @NotNull @Positive Integer position,
        @NotNull ExamResultValueType valueType,
        BigDecimal numericValue,
        String textValue,
        Long unitId,
        ExamResultIntervalType intervalType,
        BigDecimal lowerBound,
        BigDecimal upperBound,
        Boolean lowerInclusive,
        Boolean upperInclusive,
        String textInterval
) {
}
