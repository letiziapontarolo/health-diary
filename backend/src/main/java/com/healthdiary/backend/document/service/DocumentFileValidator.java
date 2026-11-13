package com.healthdiary.backend.document.service;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Set;

@Component
public class DocumentFileValidator {

    private static final long MAX_FILE_SIZE = 25L * 1024 * 1024;

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "image/jpeg",
            "image/png"
    );

    public void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Document file cannot be empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                    "Document file exceeds the 25 MB limit");
        }

        String contentType = file.getContentType();

        if (contentType == null
                || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException(
                    "Unsupported document file type");
        }

        try (InputStream inputStream = file.getInputStream()) {
            byte[] signature = inputStream.readNBytes(8);

            if (!hasValidSignature(contentType, signature)) {
                throw new IllegalArgumentException(
                        "File content does not match its declared type");
            }
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Could not validate document file", e);
        }
    }

    private boolean hasValidSignature(String contentType, byte[] signature) {
        byte[] expected = switch (contentType) {
            case "application/pdf" -> new byte[]{
                    0x25, 0x50, 0x44, 0x46, 0x2D
            };
            case "image/jpeg" -> new byte[]{
                    (byte) 0xFF, (byte) 0xD8, (byte) 0xFF
            };
            case "image/png" -> new byte[]{
                    (byte) 0x89, 0x50, 0x4E, 0x47,
                    0x0D, 0x0A, 0x1A, 0x0A
            };
            default -> new byte[0];
        };

        return signature.length >= expected.length
                && Arrays.equals(
                        Arrays.copyOf(signature, expected.length),
                        expected);
    }
}
