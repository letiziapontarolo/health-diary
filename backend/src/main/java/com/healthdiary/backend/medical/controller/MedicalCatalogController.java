package com.healthdiary.backend.medical.controller;

import com.healthdiary.backend.medical.dto.MedicalServiceResponse;
import com.healthdiary.backend.medical.service.MedicalCatalogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.healthdiary.backend.medical.entity.MedicalServiceCategory;
import org.springframework.web.bind.annotation.RequestParam;
import com.healthdiary.backend.medical.dto.MedicalServiceOptionResponse;
import org.springframework.security.core.Authentication;
import com.healthdiary.backend.medical.dto.CreateMedicalServiceRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import com.healthdiary.backend.medical.dto.UpdateMedicalServiceRequest;
import org.springframework.web.bind.annotation.PatchMapping;

import java.util.List;

@RestController
@RequestMapping("/api/medical-services")
public class MedicalCatalogController {

    private final MedicalCatalogService medicalCatalogService;

    public MedicalCatalogController(
            MedicalCatalogService medicalCatalogService) {
        this.medicalCatalogService = medicalCatalogService;
    }

    @GetMapping
    public List<MedicalServiceResponse> getDefaultServices(
            @RequestParam(required = false) MedicalServiceCategory category) {

        if (category == null) {
            return medicalCatalogService.getDefaultServices();
        }

        return medicalCatalogService.getDefaultServicesByCategory(category);
    }

    @GetMapping("/{id}/options")
    public List<MedicalServiceOptionResponse> getOptions(
            @PathVariable Long id) {

        return medicalCatalogService.getOptions(id);
    }

    @GetMapping("/mine")
    public List<MedicalServiceResponse> getUserServices(
            Authentication authentication) {

        return medicalCatalogService.getUserServices(
                authentication.getName());
    }

    @PostMapping
    public ResponseEntity<MedicalServiceResponse> createService(
            @Valid @RequestBody CreateMedicalServiceRequest request,
            Authentication authentication) {

        MedicalServiceResponse response = medicalCatalogService.createService(
                authentication.getName(),
                request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PatchMapping("/{id}")
    public MedicalServiceResponse updateService(
            @PathVariable Long id,
            @Valid @RequestBody UpdateMedicalServiceRequest request,
            Authentication authentication) {

        return medicalCatalogService.updateService(
                id,
                authentication.getName(),
                request);
    }

    @PatchMapping("/{id}/archive")
    public ResponseEntity<Void> archiveService(
            @PathVariable Long id,
            Authentication authentication) {

        medicalCatalogService.archiveService(
                id,
                authentication.getName());

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/restore")
    public ResponseEntity<Void> restoreService(
            @PathVariable Long id,
            Authentication authentication) {

        medicalCatalogService.restoreService(
                id,
                authentication.getName());

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/mine/archived")
    public List<MedicalServiceResponse> getArchivedServices(
            Authentication authentication) {

        return medicalCatalogService.getArchivedServices(
                authentication.getName());
    }
}
