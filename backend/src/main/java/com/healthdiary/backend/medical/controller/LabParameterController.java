package com.healthdiary.backend.medical.controller;

import com.healthdiary.backend.medical.dto.LabParameterResponse;
import com.healthdiary.backend.medical.service.LabParameterCatalogService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.healthdiary.backend.medical.dto.CreateLabParameterRequest;
import com.healthdiary.backend.medical.dto.UpdateLabParameterRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

@RestController
@RequestMapping("/api/lab-parameters")
public class LabParameterController {

    private final LabParameterCatalogService labParameterCatalogService;

    public LabParameterController(
            LabParameterCatalogService labParameterCatalogService) {

        this.labParameterCatalogService = labParameterCatalogService;
    }

    @GetMapping
    public List<LabParameterResponse> getDefaultParameters(
            @RequestParam Long serviceId) {

        return labParameterCatalogService
                .getDefaultParameters(serviceId);
    }

    @GetMapping("/mine")
    public List<LabParameterResponse> getUserParameters(
            @RequestParam Long serviceId,
            Authentication authentication) {

        return labParameterCatalogService
                .getUserParameters(serviceId, authentication.getName());
    }

    @PostMapping
    public ResponseEntity<LabParameterResponse> createParameter(
            @Valid @RequestBody CreateLabParameterRequest request,
            Authentication authentication) {

        LabParameterResponse response = labParameterCatalogService.createParameter(
                authentication.getName(), request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{id}")
    public LabParameterResponse updateParameter(
            @PathVariable Long id,
            @Valid @RequestBody UpdateLabParameterRequest request,
            Authentication authentication) {

        return labParameterCatalogService.updateParameter(
                id, authentication.getName(), request);
    }

    @PatchMapping("/{id}/archive")
    public ResponseEntity<Void> archiveParameter(
            @PathVariable Long id,
            Authentication authentication) {

        labParameterCatalogService.archiveParameter(
                id, authentication.getName());

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/restore")
    public ResponseEntity<Void> restoreParameter(
            @PathVariable Long id,
            Authentication authentication) {

        labParameterCatalogService.restoreParameter(
                id, authentication.getName());

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/mine/archived")
    public List<LabParameterResponse> getArchivedParameters(
            Authentication authentication) {

        return labParameterCatalogService
                .getArchivedParameters(authentication.getName());
    }

}
