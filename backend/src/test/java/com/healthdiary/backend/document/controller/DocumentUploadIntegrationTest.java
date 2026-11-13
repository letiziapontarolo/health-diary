package com.healthdiary.backend.document.controller;

import tools.jackson.databind.ObjectMapper;
import com.healthdiary.backend.auth.entity.User;
import com.healthdiary.backend.auth.repository.UserRepository;
import com.healthdiary.backend.document.dto.CreateDocumentRequest;
import com.healthdiary.backend.document.repository.DocumentRepository;
import com.healthdiary.backend.document.entity.DocumentCategory;
import com.healthdiary.backend.document.repository.DocumentCategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterAll;
import java.util.Comparator;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class DocumentUploadIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private DocumentCategoryRepository categoryRepository;

    private User mario;
    private User giulia;

    private static final Path TEST_STORAGE = createTestStorage();

    private static Path createTestStorage() {
        try {
            return Files.createTempDirectory("health-diary-documents-test-");
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Could not create test document storage",
                    exception);
        }
    }

    private Long categoryId(String name) {
        return categoryRepository.findAll().stream()
                .filter(category -> category.isDefault()
                        && category.getName().equals(name))
                .map(DocumentCategory::getId)
                .findFirst()
                .orElseThrow();
    }

    private String updateMetadataJson(
            String displayName,
            String categoryName,
            String additionalFields,
            boolean report) {

        return """
                {
                    "displayName": "%s",
                    "categoryId": %d,
                    %s
                    "report": %s
                }
                """.formatted(
                displayName,
                categoryId(categoryName),
                additionalFields,
                report);
    }

    private Long uploadPdf(User owner, String displayName) throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "referto.pdf",
                "application/pdf",
                "%PDF-1.7\nTest document".getBytes(StandardCharsets.UTF_8));

        CreateDocumentRequest request = new CreateDocumentRequest(
                displayName,
                categoryId("Referti"),
                LocalDate.of(2026, 11, 10),
                null,
                null,
                true);

        MockMultipartFile metadata = new MockMultipartFile(
                "metadata",
                "",
                "application/json",
                objectMapper.writeValueAsBytes(request));

        MvcResult result = mockMvc.perform(
                multipart("/api/documents")
                        .file(file)
                        .file(metadata)
                        .with(user(owner.getEmail()))
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper
                .readTree(result.getResponse().getContentAsString())
                .get("id")
                .asLong();
    }

    @DynamicPropertySource
    static void configureDocumentStorage(DynamicPropertyRegistry registry) {
        registry.add(
                "app.documents.storage-path",
                () -> TEST_STORAGE.toString());
    }

    @BeforeEach
    void setUp() {
        documentRepository.deleteAll();
        userRepository.deleteAll();
        mario = userRepository.save(new User(
                "Mario",
                "Rossi",
                "mario.documents@test.com",
                "test-password-hash",
                LocalDate.of(1995, 5, 10)));
        giulia = userRepository.save(new User(
                "Giulia",
                "Bianchi",
                "giulia.documents@test.com",
                "test-password-hash",
                LocalDate.of(1997, 8, 15)));
    }

    @AfterAll
    static void cleanUpTestStorage() throws Exception {
        if (!Files.exists(TEST_STORAGE)) {
            return;
        }

        try (Stream<Path> paths = Files.walk(TEST_STORAGE)) {
            for (Path path : paths
                    .sorted(Comparator.reverseOrder())
                    .toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    @Test
    void uploadsAndDownloadsPdf() throws Exception {
        byte[] pdfContent = "%PDF-1.7\nTest document"
                .getBytes(StandardCharsets.UTF_8);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "referto.pdf",
                "application/pdf",
                pdfContent);

        CreateDocumentRequest request = new CreateDocumentRequest(
                "Referto tiroide",
                categoryId("Referti"),
                LocalDate.of(2026, 11, 10),
                null,
                "Analisi di controllo",
                true);

        MockMultipartFile metadata = new MockMultipartFile(
                "metadata",
                "",
                "application/json",
                objectMapper.writeValueAsBytes(request));

        MvcResult uploadResult = mockMvc.perform(
                multipart("/api/documents")
                        .file(file)
                        .file(metadata)
                        .with(user(mario.getEmail()))
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.displayName")
                        .value("Referto tiroide"))
                .andExpect(jsonPath("$.originalName")
                        .value("referto.pdf"))
                .andExpect(jsonPath("$.report")
                        .value(true))
                .andReturn();

        Long documentId = objectMapper
                .readTree(uploadResult.getResponse().getContentAsString())
                .get("id")
                .asLong();

        assertTrue(documentRepository
                .findByIdAndUserId(documentId, mario.getId())
                .isPresent());

        MvcResult downloadResult = mockMvc.perform(
                get("/api/documents/" + documentId + "/file")
                        .with(user(mario.getEmail())))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        "X-Content-Type-Options", "nosniff"))
                .andReturn();

        assertArrayEquals(
                pdfContent,
                downloadResult.getResponse().getContentAsByteArray());
    }

    @Test
    void preventsAnotherUserFromViewingDocument() throws Exception {
        Long documentId = uploadPdf(mario, "Documento privato");

        mockMvc.perform(get("/api/documents/" + documentId)
                .with(user(giulia.getEmail())))
                .andExpect(status().isNotFound());
    }

    @Test
    void preventsAnotherUserFromDownloadingDocument() throws Exception {
        Long documentId = uploadPdf(mario, "Documento privato");

        mockMvc.perform(get("/api/documents/" + documentId + "/file")
                .with(user(giulia.getEmail())))
                .andExpect(status().isNotFound());
    }

    @Test
    void returnsOnlyAuthenticatedUsersDocuments() throws Exception {
        uploadPdf(mario, "Documento di Mario");
        uploadPdf(giulia, "Documento di Giulia");

        mockMvc.perform(get("/api/documents")
                .with(user(mario.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].displayName")
                        .value("Documento di Mario"));
    }

    @Test
    void replacesDocumentFileWithoutChangingItsId() throws Exception {
        Long documentId = uploadPdf(mario, "Referto originale");

        byte[] replacementContent = "%PDF-1.7\nUpdated document".getBytes(StandardCharsets.UTF_8);

        MockMultipartFile replacement = new MockMultipartFile(
                "file",
                "referto-aggiornato.pdf",
                "application/pdf",
                replacementContent);

        mockMvc.perform(
                multipart("/api/documents/" + documentId + "/file")
                        .file(replacement)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        })
                        .with(user(mario.getEmail()))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(documentId))
                .andExpect(jsonPath("$.originalName")
                        .value("referto-aggiornato.pdf"));

        MvcResult download = mockMvc.perform(
                get("/api/documents/" + documentId + "/file")
                        .with(user(mario.getEmail())))
                .andExpect(status().isOk())
                .andReturn();

        assertArrayEquals(
                replacementContent,
                download.getResponse().getContentAsByteArray());
    }

    @Test
    void deletesDocumentAndMakesItUnavailable() throws Exception {
        Long documentId = uploadPdf(mario, "Documento da eliminare");

        mockMvc.perform(delete("/api/documents/" + documentId)
                .with(user(mario.getEmail()))
                .with(csrf()))
                .andExpect(status().isNoContent());

        assertTrue(documentRepository.findById(documentId).isEmpty());

        mockMvc.perform(get("/api/documents/" + documentId)
                .with(user(mario.getEmail())))
                .andExpect(status().isNotFound());
    }

    @Test
    void deletesPhysicalFileAfterDocumentDeletion() throws Exception {
        Long documentId = uploadPdf(mario, "Documento da eliminare");

        String storageKey = documentRepository.findById(documentId)
                .orElseThrow()
                .getStorageKey();

        Path filePath = TEST_STORAGE.resolve(storageKey);

        assertTrue(Files.exists(filePath));

        mockMvc.perform(delete("/api/documents/" + documentId)
                .with(user(mario.getEmail()))
                .with(csrf()))
                .andExpect(status().isNoContent());

        assertFalse(documentRepository.existsById(documentId));
        assertFalse(Files.exists(filePath));
    }

    @Test
    void deletesOldPhysicalFileAfterReplacement() throws Exception {
        Long documentId = uploadPdf(mario, "Referto originale");

        String oldStorageKey = documentRepository.findById(documentId)
                .orElseThrow()
                .getStorageKey();

        Path oldFilePath = TEST_STORAGE.resolve(oldStorageKey);

        assertTrue(Files.exists(oldFilePath));

        MockMultipartFile replacement = new MockMultipartFile(
                "file",
                "nuovo-referto.pdf",
                "application/pdf",
                "%PDF-1.7\nNuovo contenuto".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(
                multipart("/api/documents/" + documentId + "/file")
                        .file(replacement)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        })
                        .with(user(mario.getEmail()))
                        .with(csrf()))
                .andExpect(status().isOk());

        String newStorageKey = documentRepository.findById(documentId)
                .orElseThrow()
                .getStorageKey();

        assertNotEquals(oldStorageKey, newStorageKey);
        assertFalse(Files.exists(oldFilePath));
        assertTrue(Files.exists(TEST_STORAGE.resolve(newStorageKey)));
    }

    @Test
    void filtersDocumentsByName() throws Exception {
        uploadPdf(mario, "Ecografia tiroide");
        uploadPdf(mario, "Analisi del sangue");
        uploadPdf(mario, "Visita endocrinologica");

        mockMvc.perform(get("/api/documents")
                .with(user(mario.getEmail()))
                .param("name", "tiroide"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].displayName")
                        .value("Ecografia tiroide"));
    }

    @Test
    void filtersDocumentsByCategory() throws Exception {
        uploadPdf(mario, "Referto A");
        uploadPdf(mario, "Referto B");

        mockMvc.perform(get("/api/documents")
                .with(user(mario.getEmail()))
                .param("categoryId", categoryId("Referti").toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        mockMvc.perform(get("/api/documents")
                .with(user(mario.getEmail()))
                .param("categoryId", categoryId("Ricette").toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void keepsFilteredDocumentsPrivate() throws Exception {
        uploadPdf(mario, "Documento privato");
        uploadPdf(giulia, "Documento privato");

        mockMvc.perform(get("/api/documents")
                .with(user(mario.getEmail()))
                .param("name", "Documento"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void filtersDocumentsByDateRange() throws Exception {
        uploadPdf(mario, "Documento novembre");

        mockMvc.perform(get("/api/documents")
                .with(user(mario.getEmail()))
                .param("fromDate", "2026-11-01")
                .param("toDate", "2026-11-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(get("/api/documents")
                .with(user(mario.getEmail()))
                .param("fromDate", "2026-12-01")
                .param("toDate", "2026-12-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void rejectsInvalidDocumentDateRange() throws Exception {
        mockMvc.perform(get("/api/documents")
                .with(user(mario.getEmail()))
                .param("fromDate", "2026-12-31")
                .param("toDate", "2026-12-01"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void filtersDocumentsByNotes() throws Exception {
        uploadPdf(mario, "Referto tiroide");

        mockMvc.perform(get("/api/documents")
                .with(user(mario.getEmail()))
                .param("notes", "controllo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void findsDocumentsByNotes() throws Exception {
        Long thyroidDocumentId = uploadPdf(mario, "Referto tiroide");
        uploadPdf(mario, "Referto generico");

        mockMvc.perform(put("/api/documents/" + thyroidDocumentId)
                .with(user(mario.getEmail()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateMetadataJson(
                        "Referto tiroide",
                        "Referti",
                        "\"documentDate\": \"2026-11-10\", \"notes\": \"Controllo endocrinologico\",",
                        true)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/documents")
                .with(user(mario.getEmail()))
                .param("notes", "ENDOCRINOLOGICO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(thyroidDocumentId));
    }

    @Test
    void filtersOnlyExpiredDocuments() throws Exception {
        Long expiredId = uploadPdf(mario, "Ricetta scaduta");
        Long validId = uploadPdf(mario, "Ricetta valida");

        mockMvc.perform(put("/api/documents/" + expiredId)
                .with(user(mario.getEmail()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateMetadataJson(
                        "Ricetta scaduta",
                        "Ricette",
                        "\"expiryDate\": \"2020-01-01\",",
                        false)))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/documents/" + validId)
                .with(user(mario.getEmail()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateMetadataJson(
                        "Ricetta valida",
                        "Ricette",
                        "\"expiryDate\": \"2099-12-31\",",
                        false)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/documents")
                .with(user(mario.getEmail()))
                .param("expiredOnly", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(expiredId))
                .andExpect(jsonPath("$[0].expired").value(true));
    }

    @Test
    void ordersDocumentsByDateDescendingWithNullsLast() throws Exception {
        Long olderId = uploadPdf(mario, "Documento vecchio");
        Long newerId = uploadPdf(mario, "Documento recente");
        Long undatedId = uploadPdf(mario, "Documento senza data");

        mockMvc.perform(put("/api/documents/" + olderId)
                .with(user(mario.getEmail()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateMetadataJson(
                        "Documento vecchio",
                        "Referti",
                        "\"documentDate\": \"2025-01-10\",",
                        true)))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/documents/" + newerId)
                .with(user(mario.getEmail()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateMetadataJson(
                        "Documento recente",
                        "Referti",
                        "\"documentDate\": \"2026-11-10\",",
                        true)))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/documents/" + undatedId)
                .with(user(mario.getEmail()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateMetadataJson(
                        "Documento senza data",
                        "Referti",
                        "",
                        true)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/documents")
                .with(user(mario.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].id").value(newerId))
                .andExpect(jsonPath("$[1].id").value(olderId))
                .andExpect(jsonPath("$[2].id").value(undatedId));
    }

    @Test
    void combinesDocumentFilters() throws Exception {
        Long matchingId = uploadPdf(mario, "Ecografia tiroide");
        Long otherId = uploadPdf(mario, "Analisi sangue");

        mockMvc.perform(get("/api/documents")
                .with(user(mario.getEmail()))
                .param("name", "tiroide")
                .param("categoryId", categoryId("Referti").toString())
                .param("fromDate", "2026-11-01")
                .param("toDate", "2026-11-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(matchingId));

        assertNotEquals(matchingId, otherId);
    }

}
