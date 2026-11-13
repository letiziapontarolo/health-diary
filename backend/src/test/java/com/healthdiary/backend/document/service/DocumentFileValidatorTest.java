package com.healthdiary.backend.document.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.*;

class DocumentFileValidatorTest {

    private DocumentFileValidator validator;

    @BeforeEach
    void setUp() {
        validator = new DocumentFileValidator();
    }

    @Test
    void acceptsValidPdf() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "report.pdf",
                "application/pdf",
                "%PDF-1.7\nTest document".getBytes()
        );

        assertDoesNotThrow(() -> validator.validate(file));
    }

    @Test
    void acceptsValidJpeg() {
        byte[] content = {
                (byte) 0xFF, (byte) 0xD8, (byte) 0xFF,
                (byte) 0xE0, 0x00, 0x10
        };

        MockMultipartFile file = new MockMultipartFile(
                "file", "image.jpg", "image/jpeg", content
        );

        assertDoesNotThrow(() -> validator.validate(file));
    }

    @Test
    void acceptsValidPng() {
        byte[] content = {
                (byte) 0x89, 0x50, 0x4E, 0x47,
                0x0D, 0x0A, 0x1A, 0x0A
        };

        MockMultipartFile file = new MockMultipartFile(
                "file", "image.png", "image/png", content
        );

        assertDoesNotThrow(() -> validator.validate(file));
    }

    @Test
    void rejectsEmptyFile() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "empty.pdf", "application/pdf", new byte[0]
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> validator.validate(file)
        );
    }

    @Test
    void rejectsUnsupportedContentType() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "script.txt", "text/plain", "Hello".getBytes()
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> validator.validate(file)
        );
    }

    @Test
    void rejectsInvalidSignature() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "fake.pdf",
                "application/pdf",
                "This is not a PDF".getBytes()
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> validator.validate(file)
        );
    }

    @Test
    void rejectsFileLargerThan25Mb() {
        byte[] content = new byte[25 * 1024 * 1024 + 1];

        MockMultipartFile file = new MockMultipartFile(
                "file", "large.pdf", "application/pdf", content
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> validator.validate(file)
        );
    }
}
