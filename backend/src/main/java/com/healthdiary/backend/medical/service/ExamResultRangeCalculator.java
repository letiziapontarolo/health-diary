package com.healthdiary.backend.medical.service;

import com.healthdiary.backend.medical.entity.ExamResultIntervalType;
import com.healthdiary.backend.medical.entity.ExamResultValueType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ExamResultRangeCalculator {

    public Boolean isOutOfRange(
            ExamResultValueType valueType,
            BigDecimal numericValue,
            ExamResultIntervalType intervalType,
            BigDecimal lowerBound,
            BigDecimal upperBound,
            Boolean lowerInclusive,
            Boolean upperInclusive) {

        if (valueType != ExamResultValueType.NUMERIC
                || numericValue == null
                || intervalType == null) {
            return null;
        }

        return switch (intervalType) {
            case RANGE -> {
                if (lowerBound == null || upperBound == null) {
                    yield null;
                }

                yield isBelowLowerBound(
                        numericValue, lowerBound, lowerInclusive)
                        || isAboveUpperBound(
                        numericValue, upperBound, upperInclusive);
            }

            case LOWER_BOUND -> lowerBound == null
                    ? null
                    : isBelowLowerBound(
                            numericValue, lowerBound, lowerInclusive);

            case UPPER_BOUND -> upperBound == null
                    ? null
                    : isAboveUpperBound(
                            numericValue, upperBound, upperInclusive);

            case TEXT -> null;
        };
    }

    private boolean isBelowLowerBound(
            BigDecimal value,
            BigDecimal lowerBound,
            Boolean inclusive) {

        int comparison = value.compareTo(lowerBound);

        return comparison < 0
                || (comparison == 0 && Boolean.FALSE.equals(inclusive));
    }

    private boolean isAboveUpperBound(
            BigDecimal value,
            BigDecimal upperBound,
            Boolean inclusive) {

        int comparison = value.compareTo(upperBound);

        return comparison > 0
                || (comparison == 0 && Boolean.FALSE.equals(inclusive));
    }
}
