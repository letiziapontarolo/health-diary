package com.healthdiary.backend.medical.dto;

import java.time.LocalDate;
import java.util.List;

public record LabExamComparisonResponse(
        Long firstExamId,
        LocalDate firstExamDate,
        Long secondExamId,
        LocalDate secondExamDate,
        List<ComparedLabResult> results
) {
    public record ComparedLabResult(
            Long parameterId,
            String parameterName,
            Integer occurrence,
            ExamResultResponse first,
            ExamResultResponse second,
            boolean unitsMatch
    ) {}
}
