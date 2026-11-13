package com.healthdiary.backend.medical.dto;

import com.healthdiary.backend.medical.entity.ExamResultIntervalType;
import com.healthdiary.backend.medical.entity.ExamResultValueType;

import java.math.BigDecimal;

public record ExamResultResponse(
        Long id,
        Long examId,
        Long parameterId,
        String parameterName,
        Integer position,
        ExamResultValueType valueType,
        BigDecimal numericValue,
        String textValue,
        Long unitId,
        String unitSymbol,
        ExamResultIntervalType intervalType,
        BigDecimal lowerBound,
        BigDecimal upperBound,
        Boolean lowerInclusive,
        Boolean upperInclusive,
        String textInterval,
        Boolean outOfRange
) {
}
