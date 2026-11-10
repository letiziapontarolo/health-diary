package com.healthdiary.backend.auth.dto;

public record RegisterResponse(
        Long id,
        String firstName,
        String lastName,
        String email
) {
}
