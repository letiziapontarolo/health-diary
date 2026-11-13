package com.healthdiary.backend.medical.controller;

import com.healthdiary.backend.medical.dto.MeasurementUnitResponse;
import com.healthdiary.backend.medical.service.MeasurementUnitService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/measurement-units")
public class MeasurementUnitController {

    private final MeasurementUnitService measurementUnitService;

    public MeasurementUnitController(
            MeasurementUnitService measurementUnitService) {

        this.measurementUnitService = measurementUnitService;
    }

    @GetMapping
    public List<MeasurementUnitResponse> getUnits() {
        return measurementUnitService.getUnits();
    }
}
