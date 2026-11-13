package com.healthdiary.backend.medical.service;

import com.healthdiary.backend.medical.entity.MeasurementUnit;
import com.healthdiary.backend.medical.repository.MeasurementUnitRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MeasurementUnitServiceTest {

    @Mock
    private MeasurementUnitRepository repository;

    @InjectMocks
    private MeasurementUnitService service;

    @Test
    void getUnitsReturnsMappedUnits() {
        when(repository.findAllByOrderBySymbolAsc())
                .thenReturn(List.of(
                        new MeasurementUnit("mg/dL", "Milligrams per deciliter"),
                        new MeasurementUnit("mmol/L", "Millimoles per liter")));

        var result = service.getUnits();

        assertEquals(2, result.size());
        assertEquals("mg/dL", result.get(0).symbol());
        assertEquals("mmol/L", result.get(1).symbol());
    }

    @Test
    void getUnitsReturnsEmptyListWhenNoUnitsExist() {
        when(repository.findAllByOrderBySymbolAsc())
                .thenReturn(List.of());

        var result = service.getUnits();

        assertTrue(result.isEmpty());
    }
}
