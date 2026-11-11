package com.healthdiary.backend.medical.dto;

import com.healthdiary.backend.medical.entity.MedicalServiceCategory;
import com.healthdiary.backend.medical.entity.OptionSelectionMode;

public record MedicalServiceResponse(
        Long id,
        String name,
        MedicalServiceCategory category,
        String iconKey,
        OptionSelectionMode optionSelectionMode
) {
}