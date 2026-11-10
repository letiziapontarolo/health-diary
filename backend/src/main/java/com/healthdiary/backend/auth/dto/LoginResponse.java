package com.healthdiary.backend.auth.dto;

public record LoginResponse(
        Long id,
        String firstName,
        String lastName,
        String email
) {
}
