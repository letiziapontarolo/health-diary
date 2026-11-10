package com.healthdiary.backend.auth.service;

public class EmailAlreadyExistsException extends RuntimeException {

    public EmailAlreadyExistsException() {
        super("An account with this email already exists");
    }
}
