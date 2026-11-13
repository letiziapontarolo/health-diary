package com.healthdiary.backend.medical.repository;

import com.healthdiary.backend.medical.entity.MeasurementUnit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MeasurementUnitRepository
        extends JpaRepository<MeasurementUnit, Long> {

    List<MeasurementUnit> findAllByOrderBySymbolAsc();

    Optional<MeasurementUnit> findBySymbolIgnoreCase(String symbol);
}
