package com.healthdiary.backend.medical.dto;

import java.math.BigDecimal;

public record OrganismResponse(Long id, Long examResultId, String organismType, String organismName,
        Integer position, String observedForm, BigDecimal quantityNumeric, String quantityUnit,
        String quantityText, String antimicrobialSusceptibility, String identificationMethod, String notes) {
}
