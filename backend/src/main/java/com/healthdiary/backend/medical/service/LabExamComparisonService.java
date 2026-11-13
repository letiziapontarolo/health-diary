package com.healthdiary.backend.medical.service;

import com.healthdiary.backend.auth.entity.User;
import com.healthdiary.backend.auth.repository.UserRepository;
import com.healthdiary.backend.medical.dto.ExamResultResponse;
import com.healthdiary.backend.medical.dto.LabExamComparisonResponse;
import com.healthdiary.backend.medical.entity.*;
import com.healthdiary.backend.medical.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@Service
public class LabExamComparisonService {
    private final UserRepository users;
    private final MedicalEventRepository events;
    private final ExamResultService resultService;

    public LabExamComparisonService(UserRepository users, MedicalEventRepository events,
                                    ExamResultService resultService) {
        this.users = users;
        this.events = events;
        this.resultService = resultService;
    }

    @Transactional(readOnly = true)
    public LabExamComparisonResponse compare(Long firstId, Long secondId, String email) {
        if (firstId.equals(secondId))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose two different exams");
        User user = users.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        MedicalEvent first = ownedLaboratoryExam(firstId, user.getId());
        MedicalEvent second = ownedLaboratoryExam(secondId, user.getId());

        // Compare only results belonging to the same laboratory service.
        if (!first.getService().getId().equals(second.getService().getId()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Exams must belong to the same laboratory service");

        List<ExamResultResponse> left = resultService.list(firstId, email);
        List<ExamResultResponse> right = resultService.list(secondId, email);
        Map<Key, ExamResultResponse> leftMap = index(left);
        Map<Key, ExamResultResponse> rightMap = index(right);
        LinkedHashSet<Key> keys = new LinkedHashSet<>(leftMap.keySet());
        keys.addAll(rightMap.keySet());

        List<LabExamComparisonResponse.ComparedLabResult> comparisons = new ArrayList<>();
        for (Key key : keys) {
            ExamResultResponse a = leftMap.get(key);
            ExamResultResponse b = rightMap.get(key);
            String name = a != null ? a.parameterName() : b.parameterName();
            boolean unitsMatch = a != null && b != null
                    && Objects.equals(a.unitId(), b.unitId());
            comparisons.add(new LabExamComparisonResponse.ComparedLabResult(
                    key.parameterId(), name, key.occurrence(), a, b, unitsMatch));
        }
        return new LabExamComparisonResponse(firstId, first.getEventDate(), secondId,
                second.getEventDate(), comparisons);
    }

    private MedicalEvent ownedLaboratoryExam(Long id, Long userId) {
        MedicalEvent exam = events.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Exam not found"));
        if (exam.getService().getCategory() != MedicalServiceCategory.LABORATORY)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not a laboratory exam");
        return exam;
    }

    private Map<Key, ExamResultResponse> index(List<ExamResultResponse> results) {
        Map<Long, Integer> counts = new HashMap<>();
        Map<Key, ExamResultResponse> indexed = new LinkedHashMap<>();
        for (ExamResultResponse result : results) {
            int occurrence = counts.merge(result.parameterId(), 1, Integer::sum);
            indexed.put(new Key(result.parameterId(), occurrence), result);
        }
        return indexed;
    }

    private record Key(Long parameterId, int occurrence) {}
}
