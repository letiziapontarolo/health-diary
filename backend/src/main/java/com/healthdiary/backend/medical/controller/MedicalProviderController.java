package com.healthdiary.backend.medical.controller;

import com.healthdiary.backend.medical.dto.*;
import com.healthdiary.backend.medical.service.MedicalProviderService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api")
public class MedicalProviderController {

    private final MedicalProviderService providerService;

    public MedicalProviderController(
            MedicalProviderService providerService
    ) {
        this.providerService = providerService;
    }

    @PostMapping("/doctors")
    @ResponseStatus(HttpStatus.CREATED)
    public DoctorResponse createDoctor(
            @Valid @RequestBody CreateDoctorRequest request,
            Principal principal
    ) {
        return providerService.createDoctor(
                principal.getName(), request
        );
    }

    @GetMapping("/doctors")
    public List<DoctorResponse> getDoctors(Principal principal) {
        return providerService.getDoctors(principal.getName());
    }

    @PostMapping("/facilities")
    @ResponseStatus(HttpStatus.CREATED)
    public FacilityResponse createFacility(
            @Valid @RequestBody CreateFacilityRequest request,
            Principal principal
    ) {
        return providerService.createFacility(
                principal.getName(), request
        );
    }

    @GetMapping("/facilities")
    public List<FacilityResponse> getFacilities(Principal principal) {
        return providerService.getFacilities(principal.getName());
    }
}
