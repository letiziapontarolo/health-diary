package com.healthdiary.backend.document.controller;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Transactional
class DocumentSecurityIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:17");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void rejectsUnauthenticatedDocumentList() throws Exception {
        mockMvc.perform(get("/api/documents"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsUnauthenticatedDocumentDetails() throws Exception {
        mockMvc.perform(get("/api/documents/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsUnauthenticatedDocumentDownload() throws Exception {
        mockMvc.perform(get("/api/documents/1/file"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsUnauthenticatedDocumentDeletion() throws Exception {
        mockMvc.perform(delete("/api/documents/1")
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsDeletionWithoutCsrfToken() throws Exception {
        mockMvc.perform(delete("/api/documents/1")
                        .with(user("user@example.com")))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectsMetadataUpdateWithoutCsrfToken() throws Exception {
        mockMvc.perform(put("/api/documents/1")
                        .with(user("user@example.com"))
                        .contentType("application/json")
                        .content("""
                                {
                                    "displayName": "Referto",
                                    "category": "Referti",
                                    "report": true
                                }
                                """))
                .andExpect(status().isForbidden());
    }
}
