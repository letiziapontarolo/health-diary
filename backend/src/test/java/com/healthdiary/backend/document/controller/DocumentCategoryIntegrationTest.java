package com.healthdiary.backend.document.controller;

import com.healthdiary.backend.auth.entity.User;
import com.healthdiary.backend.auth.repository.UserRepository;
import com.healthdiary.backend.document.entity.DocumentCategory;
import com.healthdiary.backend.document.repository.DocumentCategoryRepository;
import com.healthdiary.backend.document.repository.DocumentRepository;
import com.healthdiary.backend.document.entity.Document;
import java.nio.charset.StandardCharsets;
import org.springframework.mock.web.MockMultipartFile;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class DocumentCategoryIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private DocumentCategoryRepository categoryRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private User mario;
    private User giulia;

    @BeforeEach
    void setUp() {
        documentRepository.deleteAll();

        categoryRepository.findAll().stream()
                .filter(category -> !category.isDefault())
                .forEach(categoryRepository::delete);

        categoryRepository.flush();
        userRepository.deleteAll();

        mario = userRepository.save(new User(
                "Mario",
                "Rossi",
                "mario.categories@test.com",
                "test-password-hash",
                LocalDate.of(1995, 5, 10)));

        giulia = userRepository.save(new User(
                "Giulia",
                "Bianchi",
                "giulia.categories@test.com",
                "test-password-hash",
                LocalDate.of(1997, 8, 15)));
    }

    private static final java.nio.file.Path TEST_STORAGE = createTestStorage();

    private static java.nio.file.Path createTestStorage() {
        try {
            return java.nio.file.Files.createTempDirectory(
                    "health-diary-category-test-");
        } catch (java.io.IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    @org.springframework.test.context.DynamicPropertySource
    static void configureStorage(
            org.springframework.test.context.DynamicPropertyRegistry registry) {

        registry.add(
                "app.documents.storage-path",
                () -> TEST_STORAGE.toString());
    }

    @org.junit.jupiter.api.AfterAll
    static void cleanUpStorage() throws java.io.IOException {
        try (var paths = java.nio.file.Files.walk(TEST_STORAGE)) {
            for (var path : paths
                    .sorted(java.util.Comparator.reverseOrder())
                    .toList()) {
                java.nio.file.Files.deleteIfExists(path);
            }
        }
    }

    private DocumentCategory createCategory(User owner, String name) {
        return categoryRepository.saveAndFlush(
                new DocumentCategory(owner, name));
    }

    private DocumentCategory defaultCategory(String name) {
        return categoryRepository.findAll().stream()
                .filter(category -> category.isDefault()
                        && category.getName().equals(name))
                .findFirst()
                .orElseThrow();
    }

    @Test
    void returnsTenDefaultCategories() throws Exception {
        mockMvc.perform(get("/api/document-categories")
                .with(user(mario.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(10))
                .andExpect(jsonPath("$[?(@.name == 'Referti')].isDefault")
                        .value(true));
    }

    @Test
    void returnsOnlyAccessibleCategories() throws Exception {
        createCategory(mario, "Assicurazioni");
        createCategory(giulia, "Documenti personali");

        mockMvc.perform(get("/api/document-categories")
                .with(user(mario.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(11))
                .andExpect(jsonPath("$[?(@.name == 'Assicurazioni')]")
                        .exists())
                .andExpect(jsonPath("$[?(@.name == 'Documenti personali')]")
                        .doesNotExist());
    }

    @Test
    void createsCustomCategory() throws Exception {
        mockMvc.perform(post("/api/document-categories")
                .with(user(mario.getEmail()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "name": "  Assicurazioni  "
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Assicurazioni"))
                .andExpect(jsonPath("$.isDefault").value(false))
                .andExpect(jsonPath("$.id").isNumber());

        assertTrue(categoryRepository
                .existsAccessibleCategoryWithName(
                        "Assicurazioni", mario.getId()));
    }

    @Test
    void rejectsDuplicateDefaultCategoryName() throws Exception {
        mockMvc.perform(post("/api/document-categories")
                .with(user(mario.getEmail()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "name": "  referti  "
                        }
                        """))
                .andExpect(status().isConflict());
    }

    @Test
    void rejectsDuplicateCustomCategoryName() throws Exception {
        createCategory(mario, "Assicurazioni");

        mockMvc.perform(post("/api/document-categories")
                .with(user(mario.getEmail()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "name": "ASSICURAZIONI"
                        }
                        """))
                .andExpect(status().isConflict());
    }

    @Test
    void allowsDifferentUsersToUseSameCustomName() throws Exception {
        createCategory(giulia, "Assicurazioni");

        mockMvc.perform(post("/api/document-categories")
                .with(user(mario.getEmail()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "name": "Assicurazioni"
                        }
                        """))
                .andExpect(status().isCreated());
    }

    @Test
    void renamesOwnCategory() throws Exception {
        DocumentCategory category = createCategory(mario, "Assicurazioni");

        mockMvc.perform(put("/api/document-categories/" + category.getId())
                .with(user(mario.getEmail()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "name": "Documenti assicurativi"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name")
                        .value("Documenti assicurativi"));

        assertEquals(
                "Documenti assicurativi",
                categoryRepository.findById(category.getId())
                        .orElseThrow()
                        .getName());
    }

    @Test
    void rejectsRenamingDefaultCategory() throws Exception {
        DocumentCategory category = defaultCategory("Referti");

        mockMvc.perform(put("/api/document-categories/" + category.getId())
                .with(user(mario.getEmail()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "name": "Nuovo nome"
                        }
                        """))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsRenamingAnotherUsersCategory() throws Exception {
        DocumentCategory category = createCategory(giulia, "Documenti personali");

        mockMvc.perform(put("/api/document-categories/" + category.getId())
                .with(user(mario.getEmail()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "name": "Nuovo nome"
                        }
                        """))
                .andExpect(status().isNotFound());
    }

    @Test
    void deletesUnusedCustomCategory() throws Exception {
        DocumentCategory category = createCategory(mario, "Assicurazioni");

        mockMvc.perform(delete("/api/document-categories/" + category.getId())
                .with(user(mario.getEmail()))
                .with(csrf()))
                .andExpect(status().isNoContent());

        assertFalse(categoryRepository.existsById(category.getId()));
    }

    @Test
    void rejectsDeletingDefaultCategory() throws Exception {
        DocumentCategory category = defaultCategory("Referti");

        mockMvc.perform(delete("/api/document-categories/" + category.getId())
                .with(user(mario.getEmail()))
                .with(csrf()))
                .andExpect(status().isNotFound());

        assertTrue(categoryRepository.existsById(category.getId()));
    }

    @Test
    void rejectsDeletingAnotherUsersCategory() throws Exception {
        DocumentCategory category = createCategory(giulia, "Documenti personali");

        mockMvc.perform(delete("/api/document-categories/" + category.getId())
                .with(user(mario.getEmail()))
                .with(csrf()))
                .andExpect(status().isNotFound());

        assertTrue(categoryRepository.existsById(category.getId()));
    }

    @Test
    void rejectsUnauthenticatedCategoryCreation() throws Exception {
        mockMvc.perform(post("/api/document-categories")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "name": "Assicurazioni"
                        }
                        """))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void rejectsCategoryCreationWithoutCsrf() throws Exception {
        mockMvc.perform(post("/api/document-categories")
                .with(user(mario.getEmail()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "name": "Assicurazioni"
                        }
                        """))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectsDeletingCategoryUsedByDocument() throws Exception {
        DocumentCategory category = createCategory(mario, "Assicurazioni");

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "documento.pdf",
                "application/pdf",
                "%PDF-1.7\nTest document"
                        .getBytes(StandardCharsets.UTF_8));

        String metadataJson = """
                {
                  "displayName": "Documento assicurativo",
                  "categoryId": %d,
                  "report": false
                }
                """.formatted(category.getId());

        MockMultipartFile metadata = new MockMultipartFile(
                "metadata",
                "",
                "application/json",
                metadataJson.getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/documents")
                .file(file)
                .file(metadata)
                .with(user(mario.getEmail()))
                .with(csrf()))
                .andExpect(status().isCreated());

        mockMvc.perform(delete(
                "/api/document-categories/" + category.getId())
                .with(user(mario.getEmail()))
                .with(csrf()))
                .andExpect(status().isConflict());

        assertTrue(categoryRepository.existsById(category.getId()));
    }

}
