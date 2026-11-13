package com.healthdiary.backend.document.controller;

import com.healthdiary.backend.document.dto.DocumentResponse;
import com.healthdiary.backend.document.service.DocumentService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import com.healthdiary.backend.document.dto.DocumentFilter;

class DocumentControllerTest {

    private DocumentService documentService;
    private DocumentController controller;
    private UsernamePasswordAuthenticationToken authentication;

    @BeforeEach
    void setUp() {
        documentService = mock(DocumentService.class);
        controller = new DocumentController(documentService);

        authentication = new UsernamePasswordAuthenticationToken(
                "user@example.com",
                null);
    }

    @Test
    void returnsCurrentUsersDocuments() {
        when(documentService.getFilteredDocuments(
                eq("user@example.com"),
                any(DocumentFilter.class))).thenReturn(List.of());

        List<DocumentResponse> response = controller.getDocuments(
                null, null, null, null, null, null,
                authentication);

        assertTrue(response.isEmpty());

        verify(documentService).getFilteredDocuments(
                eq("user@example.com"),
                any(DocumentFilter.class));
    }

    @Test
    void returnsDocumentById() {
        DocumentResponse document = mock(DocumentResponse.class);

        when(documentService.getDocumentById(
                10L,
                "user@example.com")).thenReturn(document);

        DocumentResponse response = controller.getDocumentById(
                10L,
                authentication);

        assertSame(document, response);
    }

    @Test
    void deletesDocument() {
        controller.deleteDocument(10L, authentication);

        verify(documentService).deleteDocument(
                10L,
                "user@example.com");
    }

    @Test
    void createsDocumentWithCreatedStatus() {
        var metadata = new com.healthdiary.backend.document.dto.CreateDocumentRequest(
                "Referto",
                1L,
                null,
                null,
                null,
                true);

        var file = new org.springframework.mock.web.MockMultipartFile(
                "file",
                "referto.pdf",
                "application/pdf",
                "%PDF-1.7\nTest".getBytes());

        DocumentResponse document = mock(DocumentResponse.class);

        when(documentService.createDocument(
                "user@example.com",
                metadata,
                file)).thenReturn(document);

        ResponseEntity<DocumentResponse> response = controller.createDocument(
                metadata,
                file,
                authentication);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertSame(document, response.getBody());
    }
}
