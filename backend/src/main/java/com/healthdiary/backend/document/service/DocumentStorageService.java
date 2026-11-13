package com.healthdiary.backend.document.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

import java.net.MalformedURLException;
import java.nio.file.LinkOption;

@Service
public class DocumentStorageService {

    private final Path storageDirectory;
    private final DocumentFileValidator fileValidator;

    public DocumentStorageService(
            @Value("${app.documents.storage-path}") String storagePath,
            DocumentFileValidator fileValidator) {

        this.fileValidator = fileValidator;
        this.storageDirectory = Path.of(storagePath)
                .toAbsolutePath()
                .normalize();

        try {
            Files.createDirectories(storageDirectory);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Could not initialize document storage directory", e);
        }
    }

    public String store(MultipartFile file) {
        fileValidator.validate(file);
        String storageKey = UUID.randomUUID().toString();
        Path destination = resolveStorageKey(storageKey);

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(
                    inputStream, destination);

            return storageKey;
        } catch (IOException e) {
            try {
                Files.deleteIfExists(destination);
            } catch (IOException cleanupException) {
                e.addSuppressed(cleanupException);
            }

            throw new IllegalStateException(
                    "Could not store document file", e);
        }
    }

    public Resource load(String storageKey) {
        Path filePath = resolveStorageKey(storageKey);

        try {
            if (!Files.isRegularFile(filePath, LinkOption.NOFOLLOW_LINKS)) {
                throw new IllegalArgumentException("Document file not found");
            }

            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.isReadable()) {
                throw new IllegalStateException("Document file is not readable");
            }

            return resource;
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Could not load document file", e);
        }
    }

    public void delete(String storageKey) {
        Path filePath = resolveStorageKey(storageKey);

        try {
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Could not delete document file", e);
        }
    }

    private Path resolveStorageKey(String storageKey) {
        if (storageKey == null
                || !storageKey.matches(
                        "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")) {
            throw new IllegalArgumentException("Invalid document storage key");
        }

        Path filePath = storageDirectory.resolve(storageKey).normalize();

        if (!filePath.startsWith(storageDirectory)) {
            throw new IllegalArgumentException("Invalid document storage path");
        }

        return filePath;
    }

}
