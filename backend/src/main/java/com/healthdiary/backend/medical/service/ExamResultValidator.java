package com.healthdiary.backend.medical.service;

import com.healthdiary.backend.medical.dto.CreateExamResultRequest;
import com.healthdiary.backend.medical.entity.ExamResultIntervalType;
import com.healthdiary.backend.medical.entity.ExamResultValueType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;

@Component
public class ExamResultValidator {
    public void validate(CreateExamResultRequest r) {
        if (r == null || r.parameterId() == null || r.position() == null
                || r.position() <= 0 || r.valueType() == null) bad("Missing required fields");
        checkPrecision(r.numericValue());
        checkPrecision(r.lowerBound());
        checkPrecision(r.upperBound());
        switch (r.valueType()) {
            case NUMERIC -> {
                if (r.numericValue() == null || r.textValue() != null) bad("Invalid numeric result");
            }
            case TEXT -> {
                if (r.numericValue() != null || r.textValue() == null
                        || r.textValue().isBlank() || r.unitId() != null) bad("Invalid text result");
            }
            case UNAVAILABLE -> {
                if (r.numericValue() != null || r.textValue() != null || r.unitId() != null)
                    bad("Unavailable result cannot contain a value");
            }
        }
        ExamResultIntervalType t = r.intervalType();
        if (t == null) {
            if (r.lowerBound() != null || r.upperBound() != null || r.lowerInclusive() != null
                    || r.upperInclusive() != null || r.textInterval() != null)
                bad("Interval fields require an interval type");
            return;
        }
        switch (t) {
            case RANGE -> {
                if (r.lowerBound() == null || r.upperBound() == null
                        || r.lowerInclusive() == null || r.upperInclusive() == null
                        || r.textInterval() != null || r.lowerBound().compareTo(r.upperBound()) > 0)
                    bad("Invalid reference range");
                if (r.lowerBound().compareTo(r.upperBound()) == 0
                        && (!r.lowerInclusive() || !r.upperInclusive()))
                    bad("Equal bounds must be inclusive");
            }
            case LOWER_BOUND -> {
                if (r.lowerBound() == null || r.lowerInclusive() == null || r.upperBound() != null
                        || r.upperInclusive() != null || r.textInterval() != null)
                    bad("Invalid lower-bound interval");
            }
            case UPPER_BOUND -> {
                if (r.upperBound() == null || r.upperInclusive() == null || r.lowerBound() != null
                        || r.lowerInclusive() != null || r.textInterval() != null)
                    bad("Invalid upper-bound interval");
            }
            case TEXT -> {
                if (r.textInterval() == null || r.textInterval().isBlank()
                        || r.lowerBound() != null || r.upperBound() != null
                        || r.lowerInclusive() != null || r.upperInclusive() != null)
                    bad("Invalid text interval");
            }
        }
    }
    private void checkPrecision(BigDecimal n) {
        if (n != null && (n.scale() > 6 || n.precision() - n.scale() > 12))
            bad("Numeric values must fit NUMERIC(18,6)");
    }
    private void bad(String reason) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, reason);
    }
}
