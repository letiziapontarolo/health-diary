package com.healthdiary.backend.document.service;

import com.healthdiary.backend.auth.entity.User;
import com.healthdiary.backend.auth.repository.UserRepository;
import com.healthdiary.backend.document.dto.CreateDocumentRequest;
import com.healthdiary.backend.document.dto.UpdateDocumentRequest;
import com.healthdiary.backend.document.repository.DocumentRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;
import com.healthdiary.backend.document.entity.DocumentCategory;
import com.healthdiary.backend.document.repository.DocumentCategoryRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.healthdiary.backend.document.entity.Document;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronization;

class DocumentServiceTest {

    private DocumentRepository documentRepository;
    private UserRepository userRepository;
    private DocumentStorageService storageService;
    private DocumentService documentService;
    private DocumentCategoryRepository categoryRepository;

    private Document mockExistingDocument() {
        Document document = mock(Document.class);

        DocumentCategory category = mockCategory(1L, "Referti");

        when(document.getStorageKey()).thenReturn("old-storage-key");
        when(document.getCategory()).thenReturn(category);

        return document;
    }

    @BeforeEach
    void setUp() {
        categoryRepository = mock(DocumentCategoryRepository.class);
        documentRepository = mock(DocumentRepository.class);
        userRepository = mock(UserRepository.class);
        storageService = mock(DocumentStorageService.class);

        Clock clock = Clock.fixed(
                Instant.parse("2026-11-13T12:00:00Z"),
                ZoneOffset.UTC);

        documentService = new DocumentService(
                documentRepository,
                categoryRepository,
                userRepository,
                storageService,
                clock);
    }

    private void mockDefaultCategory() {
        DocumentCategory category = mockCategory(1L, "Referti");

        when(category.isDefault()).thenReturn(true);

        when(categoryRepository.findById(1L))
                .thenReturn(Optional.of(category));
    }

    @Test
    void rejectsMissingUser() {
        when(userRepository.findByEmailIgnoreCase("missing@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(
                ResponseStatusException.class,
                () -> documentService.getDocuments("missing@example.com"));
    }

    @Test
    void rejectsMissingMetadata() {
        User user = mock(User.class);

        when(userRepository.findByEmailIgnoreCase("user@example.com"))
                .thenReturn(Optional.of(user));

        assertThrows(
                ResponseStatusException.class,
                () -> documentService.createDocument(
                        "user@example.com",
                        null,
                        validPdf()));

        verifyNoInteractions(storageService);
    }

    @Test
    void rejectsBlankDisplayName() {
        mockUser();

        CreateDocumentRequest request = new CreateDocumentRequest(
                " ",
                1L,
                null,
                null,
                null,
                true);

        assertThrows(
                ResponseStatusException.class,
                () -> documentService.createDocument(
                        "user@example.com",
                        request,
                        validPdf()));

        verifyNoInteractions(storageService);
    }

    @Test
    void rejectsMissingCategory() {
        mockUser();

        CreateDocumentRequest request = new CreateDocumentRequest(
                "Esami del sangue",
                null,
                null,
                null,
                null,
                true);

        assertThrows(
                ResponseStatusException.class,
                () -> documentService.createDocument(
                        "user@example.com",
                        request,
                        validPdf()));

        verify(categoryRepository, never()).findById(anyLong());

        verifyNoInteractions(storageService);
    }

    @Test
    void rejectsMissingFile() {
        mockUser();

        assertThrows(
                ResponseStatusException.class,
                () -> documentService.createDocument(
                        "user@example.com",
                        validRequest(),
                        null));

        verifyNoInteractions(storageService);
    }

    private void mockUser() {
        User user = mock(User.class);

        when(userRepository.findByEmailIgnoreCase("user@example.com"))
                .thenReturn(Optional.of(user));
    }

    private DocumentCategory mockCategory(Long id, String name) {
        DocumentCategory category = mock(DocumentCategory.class);

        when(category.getId()).thenReturn(id);
        when(category.getName()).thenReturn(name);

        return category;
    }

    private CreateDocumentRequest validRequest() {
        return new CreateDocumentRequest(
                "Esami del sangue",
                1L,
                null,
                null,
                null,
                true);
    }

    private MockMultipartFile validPdf() {
        return new MockMultipartFile(
                "file",
                "report.pdf",
                "application/pdf",
                "%PDF-1.7\nTest document".getBytes());
    }

    @Test
    void createsDocumentSuccessfully() {
        mockDefaultCategory();
        User user = mock(User.class);

        when(user.getId()).thenReturn(1L);
        when(userRepository.findByEmailIgnoreCase("user@example.com"))
                .thenReturn(Optional.of(user));

        when(storageService.store(any()))
                .thenReturn("generated-storage-key");

        when(documentRepository.saveAndFlush(any(Document.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.initSynchronization();

        try {
            assertDoesNotThrow(() -> documentService.createDocument(
                    "user@example.com",
                    validRequest(),
                    validPdf()));

            verify(storageService).store(any());
            verify(documentRepository).saveAndFlush(any(Document.class));

        } finally {
            TransactionSynchronizationManager.clearSynchronization();
            TransactionSynchronizationManager.setActualTransactionActive(false);
        }
    }

    @Test
    void deletesStoredFileWhenDatabaseSaveFails() {
        mockDefaultCategory();
        mockUser();

        when(storageService.store(any()))
                .thenReturn("generated-storage-key");

        when(documentRepository.saveAndFlush(any(Document.class)))
                .thenThrow(new RuntimeException("Database error"));

        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.initSynchronization();

        try {
            assertThrows(
                    RuntimeException.class,
                    () -> documentService.createDocument(
                            "user@example.com",
                            validRequest(),
                            validPdf()));

            verify(storageService).delete("generated-storage-key");

        } finally {
            TransactionSynchronizationManager.clearSynchronization();
            TransactionSynchronizationManager.setActualTransactionActive(false);
        }
    }

    @Test
    void deletesStoredFileWhenTransactionRollsBack() {
        mockDefaultCategory();
        mockUser();

        when(storageService.store(any()))
                .thenReturn("generated-storage-key");

        when(documentRepository.saveAndFlush(any(Document.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.initSynchronization();

        try {
            documentService.createDocument(
                    "user@example.com",
                    validRequest(),
                    validPdf());

            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(synchronization -> synchronization.afterCompletion(
                            org.springframework.transaction.support.TransactionSynchronization.STATUS_ROLLED_BACK));

            verify(storageService).delete("generated-storage-key");

        } finally {
            TransactionSynchronizationManager.clearSynchronization();
            TransactionSynchronizationManager.setActualTransactionActive(false);
        }
    }

    @Test
    void rejectsUploadWithoutActiveTransaction() {
        mockDefaultCategory();
        mockUser();

        assertThrows(
                IllegalStateException.class,
                () -> documentService.createDocument(
                        "user@example.com",
                        validRequest(),
                        validPdf()));

        verifyNoInteractions(storageService);
    }

    @Test
    void rejectsUpdatingAnotherUsersDocument() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);

        when(userRepository.findByEmailIgnoreCase("user@example.com"))
                .thenReturn(Optional.of(user));

        when(documentRepository.findByIdAndUserId(99L, 1L))
                .thenReturn(Optional.empty());

        UpdateDocumentRequest request = new UpdateDocumentRequest(
                "Updated name",
                1L,
                null,
                null,
                null,
                true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> documentService.updateDocument(
                        99L,
                        "user@example.com",
                        request));

        assertEquals(404, exception.getStatusCode().value());

        verify(documentRepository).findByIdAndUserId(99L, 1L);
        verifyNoInteractions(storageService);
    }

    @Test
    void rejectsBlankDisplayNameWhenUpdating() {
        User user = mock(User.class);
        Document document = mock(Document.class);

        when(user.getId()).thenReturn(1L);
        when(userRepository.findByEmailIgnoreCase("user@example.com"))
                .thenReturn(Optional.of(user));

        when(documentRepository.findByIdAndUserId(10L, 1L))
                .thenReturn(Optional.of(document));

        UpdateDocumentRequest request = new UpdateDocumentRequest(
                " ",
                1L,
                null,
                null,
                null,
                true);

        assertThrows(
                ResponseStatusException.class,
                () -> documentService.updateDocument(
                        10L,
                        "user@example.com",
                        request));

        verify(document, never()).updateMetadata(
                any(), any(), any(), any(), any(), anyBoolean());
    }

    @Test
    void rejectsDownloadingAnotherUsersDocument() {
        User user = mock(User.class);

        when(user.getId()).thenReturn(1L);
        when(userRepository.findByEmailIgnoreCase("user@example.com"))
                .thenReturn(Optional.of(user));

        when(documentRepository.findByIdAndUserId(99L, 1L))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> documentService.getDocumentFile(
                        99L,
                        "user@example.com"));

        assertEquals(404, exception.getStatusCode().value());
        verifyNoInteractions(storageService);
    }

    @Test
    void updatesDocumentMetadataSuccessfully() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);

        when(userRepository.findByEmailIgnoreCase("user@example.com"))
                .thenReturn(Optional.of(user));

        DocumentCategory originalCategory = mockCategory(1L, "Referti");

        Document document = new Document(
                user,
                "original.pdf",
                "Vecchio nome",
                "storage-key",
                "application/pdf",
                1024L,
                originalCategory,
                LocalDate.of(2026, 10, 1),
                null,
                null,
                true);

        when(documentRepository.findByIdAndUserId(10L, 1L))
                .thenReturn(Optional.of(document));

        UpdateDocumentRequest request = new UpdateDocumentRequest(
                "Nuovo nome",
                2L,
                LocalDate.of(2026, 11, 10),
                LocalDate.of(2027, 11, 10),
                "Documento aggiornato",
                false);

        DocumentCategory updatedCategory = mockCategory(2L, "Analisi");

        when(updatedCategory.isDefault()).thenReturn(true);

        when(categoryRepository.findById(2L))
                .thenReturn(Optional.of(updatedCategory));
        documentService.updateDocument(
                10L,
                "user@example.com",
                request);

        assertEquals("Nuovo nome", document.getDisplayName());
        assertSame(updatedCategory, document.getCategory());
        assertEquals("Analisi", document.getCategory().getName());
        assertEquals(
                LocalDate.of(2026, 11, 10),
                document.getDocumentDate());
        assertEquals(
                LocalDate.of(2027, 11, 10),
                document.getExpiryDate());
        assertEquals("Documento aggiornato", document.getNotes());
        assertFalse(document.isReport());

        verifyNoInteractions(storageService);
    }

    @Test
    void returnsOnlyCurrentUsersDocuments() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);

        when(userRepository.findByEmailIgnoreCase("user@example.com"))
                .thenReturn(Optional.of(user));

        when(documentRepository.findByUserIdOrderByDocumentDateDescIdDesc(1L))
                .thenReturn(List.of());

        var result = documentService.getDocuments("user@example.com");

        assertTrue(result.isEmpty());

        verify(documentRepository)
                .findByUserIdOrderByDocumentDateDescIdDesc(1L);
    }

    @Test
    void marksExpiredDocumentsInResponse() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);

        when(userRepository.findByEmailIgnoreCase("user@example.com"))
                .thenReturn(Optional.of(user));

        DocumentCategory prescriptionCategory = mockCategory(3L, "Ricette");

        Document document = new Document(
                user,
                "prescription.pdf",
                "Ricetta medica",
                "storage-key",
                "application/pdf",
                1024L,
                prescriptionCategory,
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 11, 12),
                null,
                false);

        when(documentRepository.findByIdAndUserId(10L, 1L))
                .thenReturn(Optional.of(document));

        var response = documentService.getDocumentById(
                10L,
                "user@example.com");

        assertTrue(response.expired());
    }

    @Test
    void rejectsReplacingAnotherUsersDocument() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);

        when(userRepository.findByEmailIgnoreCase("user@example.com"))
                .thenReturn(Optional.of(user));

        when(documentRepository.findByIdAndUserId(99L, 1L))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> documentService.replaceDocumentFile(
                        99L,
                        "user@example.com",
                        validPdf()));

        assertEquals(404, exception.getStatusCode().value());
        verifyNoInteractions(storageService);
    }

    @Test
    void rejectsDeletingAnotherUsersDocument() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);

        when(userRepository.findByEmailIgnoreCase("user@example.com"))
                .thenReturn(Optional.of(user));

        when(documentRepository.findByIdAndUserId(99L, 1L))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> documentService.deleteDocument(
                        99L,
                        "user@example.com"));

        assertEquals(404, exception.getStatusCode().value());
        verifyNoInteractions(storageService);
    }

    @Test
    void deletesOldFileAfterSuccessfulReplacementCommit() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);

        when(userRepository.findByEmailIgnoreCase("user@example.com"))
                .thenReturn(Optional.of(user));

        Document document = mockExistingDocument();

        when(documentRepository.findByIdAndUserId(10L, 1L))
                .thenReturn(Optional.of(document));

        when(storageService.store(any()))
                .thenReturn("new-storage-key");

        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.initSynchronization();

        try {
            documentService.replaceDocumentFile(
                    10L,
                    "user@example.com",
                    validPdf());

            verify(storageService, never()).delete("old-storage-key");

            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(synchronization -> synchronization.afterCompletion(
                            TransactionSynchronization.STATUS_COMMITTED));

            verify(storageService).delete("old-storage-key");
            verify(storageService, never()).delete("new-storage-key");

        } finally {
            TransactionSynchronizationManager.clearSynchronization();
            TransactionSynchronizationManager.setActualTransactionActive(false);
        }
    }

    @Test
    void preservesOldFileWhenReplacementRollsBack() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);

        when(userRepository.findByEmailIgnoreCase("user@example.com"))
                .thenReturn(Optional.of(user));

        Document document = mockExistingDocument();

        when(documentRepository.findByIdAndUserId(10L, 1L))
                .thenReturn(Optional.of(document));

        when(storageService.store(any()))
                .thenReturn("new-storage-key");

        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.initSynchronization();

        try {
            documentService.replaceDocumentFile(
                    10L,
                    "user@example.com",
                    validPdf());

            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(synchronization -> synchronization.afterCompletion(
                            TransactionSynchronization.STATUS_ROLLED_BACK));

            verify(storageService).delete("new-storage-key");
            verify(storageService, never()).delete("old-storage-key");

        } finally {
            TransactionSynchronizationManager.clearSynchronization();
            TransactionSynchronizationManager.setActualTransactionActive(false);
        }
    }

    @Test
    void deletesPhysicalFileOnlyAfterDeletionCommit() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);

        when(userRepository.findByEmailIgnoreCase("user@example.com"))
                .thenReturn(Optional.of(user));

        Document document = mockExistingDocument();

        when(documentRepository.findByIdAndUserId(10L, 1L))
                .thenReturn(Optional.of(document));

        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.initSynchronization();

        try {
            documentService.deleteDocument(10L, "user@example.com");

            verify(documentRepository).delete(document);
            verify(documentRepository).flush();
            verify(storageService, never()).delete(anyString());

            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(synchronization -> synchronization.afterCompletion(
                            TransactionSynchronization.STATUS_COMMITTED));

            verify(storageService).delete("old-storage-key");

        } finally {
            TransactionSynchronizationManager.clearSynchronization();
            TransactionSynchronizationManager.setActualTransactionActive(false);
        }
    }

    @Test
    void preservesPhysicalFileWhenDeletionRollsBack() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);

        when(userRepository.findByEmailIgnoreCase("user@example.com"))
                .thenReturn(Optional.of(user));

        Document document = mockExistingDocument();

        when(documentRepository.findByIdAndUserId(10L, 1L))
                .thenReturn(Optional.of(document));

        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.initSynchronization();

        try {
            documentService.deleteDocument(10L, "user@example.com");

            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(synchronization -> synchronization.afterCompletion(
                            TransactionSynchronization.STATUS_ROLLED_BACK));

            verify(storageService, never()).delete(anyString());

        } finally {
            TransactionSynchronizationManager.clearSynchronization();
            TransactionSynchronizationManager.setActualTransactionActive(false);
        }
    }

    @Test
    void deletesNewFileWhenReplacementFlushFails() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);

        when(userRepository.findByEmailIgnoreCase("user@example.com"))
                .thenReturn(Optional.of(user));

        Document document = mockExistingDocument();

        when(documentRepository.findByIdAndUserId(10L, 1L))
                .thenReturn(Optional.of(document));

        when(storageService.store(any()))
                .thenReturn("new-storage-key");

        doThrow(new RuntimeException("Database error"))
                .when(documentRepository).flush();

        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.initSynchronization();

        try {
            assertThrows(
                    RuntimeException.class,
                    () -> documentService.replaceDocumentFile(
                            10L,
                            "user@example.com",
                            validPdf()));

            verify(storageService).delete("new-storage-key");
            verify(storageService, never()).delete("old-storage-key");

        } finally {
            TransactionSynchronizationManager.clearSynchronization();
            TransactionSynchronizationManager.setActualTransactionActive(false);
        }
    }

    @Test
    void preservesPhysicalFileWhenDeletionFlushFails() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);

        when(userRepository.findByEmailIgnoreCase("user@example.com"))
                .thenReturn(Optional.of(user));

        Document document = mockExistingDocument();

        when(documentRepository.findByIdAndUserId(10L, 1L))
                .thenReturn(Optional.of(document));

        doThrow(new RuntimeException("Database error"))
                .when(documentRepository).flush();

        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.initSynchronization();

        try {
            assertThrows(
                    RuntimeException.class,
                    () -> documentService.deleteDocument(
                            10L,
                            "user@example.com"));

            verify(storageService, never()).delete(anyString());

        } finally {
            TransactionSynchronizationManager.clearSynchronization();
            TransactionSynchronizationManager.setActualTransactionActive(false);
        }
    }

    @Test
    void rejectsOriginalFilenameContainingPath() {
        mockDefaultCategory();
        mockUser();

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "../report.pdf",
                "application/pdf",
                "%PDF-1.7\nTest document".getBytes());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> documentService.createDocument(
                        "user@example.com",
                        validRequest(),
                        file));

        assertEquals(400, exception.getStatusCode().value());
        verifyNoInteractions(storageService);
    }

    @Test
    void rejectsNonexistentCategory() {
        mockUser();

        CreateDocumentRequest request = new CreateDocumentRequest(
                "Esami del sangue",
                999L,
                null,
                null,
                null,
                true);

        when(categoryRepository.findById(999L))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> documentService.createDocument(
                        "user@example.com",
                        request,
                        validPdf()));

        assertEquals(404, exception.getStatusCode().value());

        verifyNoInteractions(storageService);
    }

    @Test
    void rejectsAnotherUsersCategory() {
        User currentUser = mock(User.class);
        User otherUser = mock(User.class);

        when(currentUser.getId()).thenReturn(1L);
        when(otherUser.getId()).thenReturn(2L);

        when(userRepository.findByEmailIgnoreCase("user@example.com"))
                .thenReturn(Optional.of(currentUser));

        DocumentCategory category = mock(DocumentCategory.class);

        when(category.isDefault()).thenReturn(false);
        when(category.getUser()).thenReturn(otherUser);

        when(categoryRepository.findById(10L))
                .thenReturn(Optional.of(category));

        CreateDocumentRequest request = new CreateDocumentRequest(
                "Esami del sangue",
                10L,
                null,
                null,
                null,
                true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> documentService.createDocument(
                        "user@example.com",
                        request,
                        validPdf()));

        assertEquals(404, exception.getStatusCode().value());

        verifyNoInteractions(storageService);
    }

    @Test
    void acceptsOwnCustomCategory() {
        User user = mock(User.class);

        when(user.getId()).thenReturn(1L);

        when(userRepository.findByEmailIgnoreCase("user@example.com"))
                .thenReturn(Optional.of(user));

        DocumentCategory category = mock(DocumentCategory.class);

        when(category.isDefault()).thenReturn(false);
        when(category.getUser()).thenReturn(user);

        when(categoryRepository.findById(10L))
                .thenReturn(Optional.of(category));

        CreateDocumentRequest request = new CreateDocumentRequest(
                "Esami del sangue",
                10L,
                null,
                null,
                null,
                true);

        when(storageService.store(any()))
                .thenReturn("generated-storage-key");

        when(documentRepository.saveAndFlush(any(Document.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.initSynchronization();

        try {
            assertDoesNotThrow(() -> documentService.createDocument(
                    "user@example.com",
                    request,
                    validPdf()));

            verify(documentRepository).saveAndFlush(
                    argThat(document -> document.getCategory() == category));

        } finally {
            TransactionSynchronizationManager.clearSynchronization();
            TransactionSynchronizationManager.setActualTransactionActive(false);
        }
    }

}
