package com.healthdiary.backend.document.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

class DocumentStorageServiceTest {

    @TempDir
    Path tempDirectory;

    private DocumentStorageService createService() {
        return new DocumentStorageService(
                tempDirectory.toString(),
                new DocumentFileValidator());
    }

    private MockMultipartFile validPdf() {
        return new MockMultipartFile(
                "file",
                "report.pdf",
                "application/pdf",
                "%PDF-1.7\nTest document".getBytes());
    }

    @Test
    void storesFileWithUniqueKey() throws Exception {
        DocumentStorageService service = createService();

        String firstKey = service.store(validPdf());
        String secondKey = service.store(validPdf());

        assertNotEquals(firstKey, secondKey);
        assertTrue(Files.exists(tempDirectory.resolve(firstKey)));
        assertTrue(Files.exists(tempDirectory.resolve(secondKey)));
    }

    @Test
    void loadsStoredFile() throws Exception {
        DocumentStorageService service = createService();

        String storageKey = service.store(validPdf());
        Resource resource = service.load(storageKey);

        assertTrue(resource.exists());

        try (InputStream inputStream = resource.getInputStream()) {
            assertArrayEquals(
                    validPdf().getBytes(),
                    inputStream.readAllBytes());
        }
    }

    @Test
    void deletesStoredFile() {
        DocumentStorageService service = createService();

        String storageKey = service.store(validPdf());
        service.delete(storageKey);

        assertFalse(Files.exists(tempDirectory.resolve(storageKey)));
    }

    @Test
    void rejectsInvalidStorageKey() {
        DocumentStorageService service = createService();

        assertThrows(
                IllegalArgumentException.class,
                () -> service.load("../secret.txt"));
    }

    @Test
    void doesNotStoreInvalidFile() {
        DocumentStorageService service = createService();

        MockMultipartFile invalidFile = new MockMultipartFile(
                "file",
                "fake.pdf",
                "application/pdf",
                "Invalid content".getBytes());

        assertThrows(
                IllegalArgumentException.class,
                () -> service.store(invalidFile));
    }

    @Test
    void createsStorageDirectoryIfMissing() {
        Path storagePath = tempDirectory.resolve("documents");

        assertFalse(Files.exists(storagePath));

        new DocumentStorageService(
                storagePath.toString(),
                new DocumentFileValidator());

        assertTrue(Files.isDirectory(storagePath));
    }
}
