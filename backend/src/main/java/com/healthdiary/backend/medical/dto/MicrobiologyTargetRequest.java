package com.healthdiary.backend.medical.dto;
import jakarta.validation.constraints.*;
public record MicrobiologyTargetRequest(@NotNull @Positive Integer position,
 @NotBlank @Size(max=250) String targetName,@NotBlank String result,
 String resultText,@Size(max=100) String method,String notes) {}
