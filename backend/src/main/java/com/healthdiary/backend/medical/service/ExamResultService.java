package com.healthdiary.backend.medical.service;

import com.healthdiary.backend.auth.entity.User;
import com.healthdiary.backend.auth.repository.UserRepository;
import com.healthdiary.backend.medical.dto.*;
import com.healthdiary.backend.medical.entity.*;
import com.healthdiary.backend.medical.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@Service
public class ExamResultService {
    private final UserRepository users;
    private final MedicalEventRepository events;
    private final LabParameterRepository parameters;
    private final MeasurementUnitRepository units;
    private final ExamResultRepository results;
    private final ExamResultValidator validator;
    private final ExamResultRangeCalculator calculator;

    public ExamResultService(UserRepository users, MedicalEventRepository events,
            LabParameterRepository parameters, MeasurementUnitRepository units,
            ExamResultRepository results, ExamResultValidator validator,
            ExamResultRangeCalculator calculator) {
        this.users = users;
        this.events = events;
        this.parameters = parameters;
        this.units = units;
        this.results = results;
        this.validator = validator;
        this.calculator = calculator;
    }

    private MedicalEvent ownedLabExam(Long examId, String email) {
        User user = users.findByEmailIgnoreCase(email)
                .orElseThrow(() -> notFound("User not found"));
        MedicalEvent exam = events.findByIdAndUserId(examId, user.getId())
                .orElseThrow(() -> notFound("Exam not found"));
        if (exam.getService().getCategory() != MedicalServiceCategory.LABORATORY)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Event is not a laboratory exam");
        return exam;
    }

    private LabParameter validParameter(Long id, MedicalEvent exam) {
        LabParameter p = parameters.findById(id)
                .orElseThrow(() -> notFound("Parameter not found"));
        if (!p.isActive() || !p.getService().getId().equals(exam.getService().getId())
                || (p.getUser() != null && !p.getUser().getId().equals(exam.getUser().getId())))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parameter unavailable for this exam");
        return p;
    }

    private MeasurementUnit validUnit(Long id) {
        return id == null ? null : units.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unit not found"));
    }

    @Transactional(readOnly = true)
    public List<ExamResultResponse> list(Long examId, String email) {
        ownedLabExam(examId, email);
        return results.findByExamIdOrderByPositionAsc(examId).stream().map(this::response).toList();
    }

    @Transactional
    public ExamResultResponse create(Long examId, String email, CreateExamResultRequest request) {
        MedicalEvent exam = ownedLabExam(examId, email);
        validator.validate(request);
        if (results.existsByExamIdAndPosition(examId, request.position()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Position already used");
        LabParameter parameter = validParameter(request.parameterId(), exam);
        MeasurementUnit unit = validUnit(request.unitId());
        ExamResult result = new ExamResult(exam, parameter, request.position(), request.valueType(),
                request.numericValue(), request.textValue(), unit, request.intervalType(),
                request.lowerBound(), request.upperBound(), request.lowerInclusive(),
                request.upperInclusive(), request.textInterval());
        return response(results.save(result));
    }

    @Transactional
    public ExamResultResponse update(Long examId, Long resultId, String email,
            UpdateExamResultRequest update) {
        MedicalEvent exam = ownedLabExam(examId, email);
        ExamResult result = results.findByIdAndExamId(resultId, examId)
                .orElseThrow(() -> notFound("Result not found"));
        CreateExamResultRequest r = new CreateExamResultRequest(update.parameterId(), update.position(),
                update.valueType(), update.numericValue(), update.textValue(), update.unitId(),
                update.intervalType(), update.lowerBound(), update.upperBound(),
                update.lowerInclusive(), update.upperInclusive(), update.textInterval());
        validator.validate(r);
        if (!result.getPosition().equals(r.position())
                && results.existsByExamIdAndPosition(examId, r.position()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Position already used");
        result.update(validParameter(r.parameterId(), exam), r.position(), r.valueType(),
                r.numericValue(), r.textValue(), validUnit(r.unitId()), r.intervalType(),
                r.lowerBound(), r.upperBound(), r.lowerInclusive(), r.upperInclusive(), r.textInterval());
        return response(result);
    }

    @Transactional
    public void delete(Long examId, Long resultId, String email) {
        ownedLabExam(examId, email);
        ExamResult result = results.findByIdAndExamId(resultId, examId)
                .orElseThrow(() -> notFound("Result not found"));
        results.delete(result);
    }

    @Transactional(readOnly = true)
    public List<ExamResultResponse> trend(Long parameterId, String email) {
        User user = users.findByEmailIgnoreCase(email)
                .orElseThrow(() -> notFound("User not found"));
        LabParameter p = parameters.findById(parameterId)
                .orElseThrow(() -> notFound("Parameter not found"));
        if (p.getUser() != null && !p.getUser().getId().equals(user.getId()))
            throw notFound("Parameter not found");
        return results.findByParameterIdAndExamUserIdOrderByExamEventDateAsc(parameterId, user.getId())
                .stream().map(this::response).toList();
    }

    private ExamResultResponse response(ExamResult r) {
        MeasurementUnit unit = r.getUnit();
        return new ExamResultResponse(r.getId(), r.getExam().getId(), r.getParameter().getId(),
                r.getParameter().getName(), r.getPosition(), r.getValueType(), r.getNumericValue(),
                r.getTextValue(), unit == null ? null : unit.getId(),
                unit == null ? null : unit.getSymbol(), r.getIntervalType(), r.getLowerBound(),
                r.getUpperBound(), r.getLowerInclusive(), r.getUpperInclusive(), r.getTextInterval(),
                calculator.isOutOfRange(r.getValueType(), r.getNumericValue(), r.getIntervalType(),
                        r.getLowerBound(), r.getUpperBound(), r.getLowerInclusive(), r.getUpperInclusive()));
    }

    private ResponseStatusException notFound(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }
}
