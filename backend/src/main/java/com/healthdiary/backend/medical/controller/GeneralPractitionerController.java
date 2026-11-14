package com.healthdiary.backend.medical.controller;

import com.healthdiary.backend.medical.dto.*;
import com.healthdiary.backend.medical.service.GeneralPractitionerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/general-practitioner")
public class GeneralPractitionerController {
    private final GeneralPractitionerService service;

    public GeneralPractitionerController(GeneralPractitionerService service) {
        this.service = service;
    }

    @GetMapping("/current")
    public GeneralPractitionerResponse current(Principal principal) {
        return service.current(principal.getName());
    }

    @GetMapping("/history")
    public List<GeneralPractitionerResponse> history(Principal principal) {
        return service.history(principal.getName());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GeneralPractitionerResponse change(@Valid @RequestBody SetGeneralPractitionerRequest request,
            Principal principal) {
        return service.change(principal.getName(), request);
    }
}
