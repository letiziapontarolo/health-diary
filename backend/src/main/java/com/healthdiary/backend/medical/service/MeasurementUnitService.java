package com.healthdiary.backend.medical.service;

import com.healthdiary.backend.medical.dto.MeasurementUnitResponse;
import com.healthdiary.backend.medical.repository.MeasurementUnitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MeasurementUnitService {

    private final MeasurementUnitRepository measurementUnitRepository;

    public MeasurementUnitService(
            MeasurementUnitRepository measurementUnitRepository) {

        this.measurementUnitRepository = measurementUnitRepository;
    }

    @Transactional(readOnly = true)
    public List<MeasurementUnitResponse> getUnits() {
        return measurementUnitRepository
                .findAllByOrderBySymbolAsc()
                .stream()
                .map(unit -> new MeasurementUnitResponse(
                        unit.getId(),
                        unit.getSymbol(),
                        unit.getName()))
                .toList();
    }
}
