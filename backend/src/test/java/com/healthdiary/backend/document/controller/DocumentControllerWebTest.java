package com.healthdiary.backend.document.controller;

import com.healthdiary.backend.document.service.DocumentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.healthdiary.backend.document.dto.DocumentFilter;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class DocumentControllerWebTest {

    private DocumentService documentService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        documentService = mock(DocumentService.class);

        DocumentController controller = new DocumentController(documentService);

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new DocumentExceptionHandler())
                .build();
    }

    private UsernamePasswordAuthenticationToken authentication() {
        return new UsernamePasswordAuthenticationToken(
                "user@example.com",
                null,
                List.of());
    }

    @Test
    void returnsDocumentsAsJson() throws Exception {
        when(documentService.getFilteredDocuments(
                eq("user@example.com"),
                any(DocumentFilter.class))).thenReturn(List.of());

        mockMvc.perform(get("/api/documents")
                .principal(authentication()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON))
                .andExpect(content().json("[]"));

        verify(documentService).getFilteredDocuments(
                eq("user@example.com"),
                any(DocumentFilter.class));
    }

    @Test
    void returnsNotFoundForMissingDocument() throws Exception {
        when(documentService.getDocumentById(
                99L, "user@example.com"))
                .thenThrow(new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND,
                        "Document not found"));

        mockMvc.perform(get("/api/documents/99")
                .principal(authentication()))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsUnsupportedMetadataContentType() throws Exception {
        mockMvc.perform(post("/api/documents")
                .principal(authentication())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isUnsupportedMediaType());

        verifyNoInteractions(documentService);
    }

    @Test
    void rejectsInvalidDocumentId() throws Exception {
        mockMvc.perform(get("/api/documents/not-a-number")
                .principal(authentication()))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(documentService);
    }
}
