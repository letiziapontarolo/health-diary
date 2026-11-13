package com.healthdiary.backend.medical.dto;
public record MicrobiologyTargetResponse(Long id,Long examResultId,Integer position,String targetName,
 String result,String resultText,String method,String notes) {}
