package com.healthdiary.backend.medical.dto;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
public record OrganismRequest(
 @NotBlank String organismType, @NotBlank @Size(max=250) String organismName,
 @NotNull @Positive Integer position, @Size(max=150) String observedForm,
 @DecimalMin("0.0") BigDecimal quantityNumeric, @Size(max=50) String quantityUnit,
 String quantityText, String antimicrobialSusceptibility,
 @Size(max=150) String identificationMethod, String notes) {}
