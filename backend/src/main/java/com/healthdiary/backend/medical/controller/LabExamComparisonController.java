package com.healthdiary.backend.medical.controller;

import com.healthdiary.backend.medical.dto.LabExamComparisonResponse;
import com.healthdiary.backend.medical.service.LabExamComparisonService;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;

@RestController
@RequestMapping("/api/lab-exams")
public class LabExamComparisonController {
    private final LabExamComparisonService service;

    public LabExamComparisonController(LabExamComparisonService service) {
        this.service = service;
    }

    @GetMapping("/compare")
    public LabExamComparisonResponse compare(@RequestParam Long firstExamId,
                                              @RequestParam Long secondExamId,
                                              Principal principal) {
        return service.compare(firstExamId, secondExamId, principal.getName());
    }
}
