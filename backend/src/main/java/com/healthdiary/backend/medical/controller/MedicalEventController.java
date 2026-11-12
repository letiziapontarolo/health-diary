package com.healthdiary.backend.medical.controller;

import com.healthdiary.backend.medical.dto.CreateMedicalEventRequest;
import com.healthdiary.backend.medical.dto.MedicalEventResponse;
import com.healthdiary.backend.medical.service.MedicalEventService;
import com.healthdiary.backend.medical.dto.UpdateMedicalEventRequest;
import com.healthdiary.backend.medical.dto.UpdateMedicalEventReportRequest;
import com.healthdiary.backend.medical.dto.UpdateReportNotExpectedRequest;

import com.healthdiary.backend.medical.dto.MedicalEventFilter;
import com.healthdiary.backend.medical.entity.MedicalEventStatus;
import com.healthdiary.backend.medical.entity.MedicalServiceCategory;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/medical-events")
public class MedicalEventController {

    private final MedicalEventService medicalEventService;

    public MedicalEventController(MedicalEventService medicalEventService) {
        this.medicalEventService = medicalEventService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MedicalEventResponse createEvent(
            @Valid @RequestBody CreateMedicalEventRequest request,
            Principal principal) {
        return medicalEventService.createEvent(
                principal.getName(),
                request);
    }

    @GetMapping
    public List<MedicalEventResponse> getEvents(
            @RequestParam(required = false) MedicalServiceCategory category,

            @RequestParam(required = false) MedicalEventStatus status,

            @RequestParam(required = false) Long doctorId,

            @RequestParam(required = false) Long facilityId,

            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,

            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,

            Principal principal) {
        MedicalEventFilter filter = new MedicalEventFilter(
                category,
                status,
                doctorId,
                facilityId,
                fromDate,
                toDate);

        return medicalEventService.getFilteredEvents(
                principal.getName(),
                filter);
    }

    @GetMapping("/{id}")
    public MedicalEventResponse getEvent(
            @PathVariable Long id,
            Principal principal) {
        return medicalEventService.getEvent(
                id,
                principal.getName());
    }

    @PutMapping("/{id}")
    public MedicalEventResponse updateEvent(
            @PathVariable Long id,
            @Valid @RequestBody UpdateMedicalEventRequest request,
            Principal principal) {
        return medicalEventService.updateEvent(
                id,
                principal.getName(),
                request);
    }

    @PatchMapping("/{id}/cancel")
    public MedicalEventResponse cancelEvent(
            @PathVariable Long id,
            Principal principal) {
        return medicalEventService.cancelEvent(
                id,
                principal.getName());
    }

    @PatchMapping("/{id}/restore")
    public MedicalEventResponse restoreEvent(
            @PathVariable Long id,
            Principal principal) {
        return medicalEventService.restoreEvent(
                id,
                principal.getName());
    }

    @PutMapping("/{id}/report")
    public MedicalEventResponse updateReport(
            @PathVariable Long id,
            @Valid @RequestBody UpdateMedicalEventReportRequest request,
            Principal principal) {
        return medicalEventService.updateReport(
                id,
                principal.getName(),
                request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEvent(
            @PathVariable Long id,
            Principal principal) {
        medicalEventService.deleteEvent(
                id,
                principal.getName());
    }

    @DeleteMapping("/{id}/report")
    public MedicalEventResponse deleteReport(
            @PathVariable Long id,
            Principal principal) {
        return medicalEventService.deleteReport(
                id,
                principal.getName());
    }

    @PatchMapping("/{id}/report-not-expected")
    public MedicalEventResponse updateReportNotExpected(
            @PathVariable Long id,
            @Valid @RequestBody UpdateReportNotExpectedRequest request,
            Principal principal) {
        return medicalEventService.updateReportNotExpected(
                id,
                principal.getName(),
                request);
    }

}
