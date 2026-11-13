package com.healthdiary.backend.medical.controller;

import com.healthdiary.backend.medical.dto.*;
import com.healthdiary.backend.medical.service.ExamResultService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api")
public class ExamResultController {
    private final ExamResultService service;
    public ExamResultController(ExamResultService service) { this.service = service; }

    @GetMapping("/medical-events/{examId}/results")
    public List<ExamResultResponse> list(@PathVariable Long examId, Authentication auth) {
        return service.list(examId, auth.getName());
    }

    @PostMapping("/medical-events/{examId}/results")
    public ResponseEntity<ExamResultResponse> create(@PathVariable Long examId,
            @Valid @RequestBody CreateExamResultRequest request, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(examId, auth.getName(), request));
    }

    @PutMapping("/medical-events/{examId}/results/{resultId}")
    public ExamResultResponse update(@PathVariable Long examId, @PathVariable Long resultId,
            @Valid @RequestBody UpdateExamResultRequest request, Authentication auth) {
        return service.update(examId, resultId, auth.getName(), request);
    }

    @DeleteMapping("/medical-events/{examId}/results/{resultId}")
    public ResponseEntity<Void> delete(@PathVariable Long examId, @PathVariable Long resultId,
            Authentication auth) {
        service.delete(examId, resultId, auth.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/lab-parameters/{parameterId}/trend")
    public List<ExamResultResponse> trend(@PathVariable Long parameterId, Authentication auth) {
        return service.trend(parameterId, auth.getName());
    }
}
