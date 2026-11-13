package com.healthdiary.backend.document.controller;

import com.healthdiary.backend.auth.entity.User;
import com.healthdiary.backend.auth.repository.UserRepository;
import com.healthdiary.backend.document.entity.MedicalEventDocumentId;
import com.healthdiary.backend.document.repository.MedicalEventDocumentRepository;
import com.healthdiary.backend.document.entity.DocumentCategory;
import com.healthdiary.backend.document.repository.DocumentCategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.LocalDate;
import com.healthdiary.backend.document.entity.Document;
import com.healthdiary.backend.document.repository.DocumentRepository;

import com.healthdiary.backend.medical.entity.MedicalEvent;
import com.healthdiary.backend.medical.entity.MedicalService;
import com.healthdiary.backend.medical.repository.MedicalEventRepository;
import com.healthdiary.backend.medical.repository.MedicalServiceRepository;
import com.healthdiary.backend.medical.entity.MedicalServiceCategory;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class DocumentLinkIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private MedicalEventRepository medicalEventRepository;

    @Autowired
    private DocumentCategoryRepository categoryRepository;

    @Autowired
    private MedicalServiceRepository medicalServiceRepository;

    private Document marioDocument;
    private MedicalEvent marioEvent;
    private MedicalService marioService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MedicalEventDocumentRepository linkRepository;

    private User mario;
    private User giulia;

    private DocumentCategory getCategory(String name) {
        return categoryRepository.findAll().stream()
                .filter(category -> category.isDefault()
                        && category.getName().equals(name))
                .findFirst()
                .orElseThrow();
    }

    private Document createDocument(User owner, String displayName) {
        return documentRepository.saveAndFlush(new Document(
                owner,
                "referto.pdf",
                displayName,
                UUID.randomUUID().toString(),
                "application/pdf",
                1024L,
                getCategory("Referti"),
                LocalDate.of(2026, 11, 10),
                null,
                null,
                true));
    }

    private MedicalService createMedicalService(User owner) {
        return medicalServiceRepository.saveAndFlush(
                new MedicalService(
                        owner,
                        "Visita endocrinologica",
                        MedicalServiceCategory.SPECIALIST,
                        null));
    }

    private MedicalEvent createMedicalEvent(
            User owner,
            MedicalService service) {

        return medicalEventRepository.saveAndFlush(
                new MedicalEvent(
                        owner,
                        service,
                        LocalDate.of(2026, 11, 10),
                        null,
                        null,
                        null,
                        "Visita di controllo"));
    }

    @BeforeEach
    void setUp() {
        String uniqueId = UUID.randomUUID().toString();

        mario = userRepository.save(new User(
                "Mario",
                "Rossi",
                "mario-" + uniqueId + "@links.test",
                "test-password-hash",
                LocalDate.of(1995, 5, 10)));

        giulia = userRepository.save(new User(
                "Giulia",
                "Bianchi",
                "giulia-" + uniqueId + "@links.test",
                "test-password-hash",
                LocalDate.of(1997, 8, 15)));

        marioDocument = createDocument(mario, "Referto tiroide");

        marioService = createMedicalService(mario);
        marioEvent = createMedicalEvent(mario, marioService);
    }

    @Test
    void linksDocumentToMedicalEvent() throws Exception {
        mockMvc.perform(put(
                "/api/documents/{documentId}/events/{eventId}",
                marioDocument.getId(),
                marioEvent.getId())
                .with(user(mario.getEmail()))
                .with(csrf()))
                .andExpect(status().isNoContent());

        MedicalEventDocumentId linkId = new MedicalEventDocumentId(
                marioDocument.getId(),
                marioEvent.getId());

        assertTrue(linkRepository.existsById(linkId));
    }

    @Test
    void linkingSameDocumentTwiceDoesNotCreateDuplicates()
            throws Exception {

        String endpoint = "/api/documents/"
                + marioDocument.getId()
                + "/events/"
                + marioEvent.getId();

        mockMvc.perform(put(endpoint)
                .with(user(mario.getEmail()))
                .with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(put(endpoint)
                .with(user(mario.getEmail()))
                .with(csrf()))
                .andExpect(status().isNoContent());

        assertEquals(
                1,
                linkRepository.findByDocumentIdAndUserId(
                        marioDocument.getId(),
                        mario.getId()).size());
    }

    @Test
    void unlinkingDocumentDoesNotDeleteDocument() throws Exception {
        String endpoint = "/api/documents/"
                + marioDocument.getId()
                + "/events/"
                + marioEvent.getId();

        mockMvc.perform(put(endpoint)
                .with(user(mario.getEmail()))
                .with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete(endpoint)
                .with(user(mario.getEmail()))
                .with(csrf()))
                .andExpect(status().isNoContent());

        assertFalse(linkRepository.existsById(
                new MedicalEventDocumentId(
                        marioDocument.getId(),
                        marioEvent.getId())));

        assertTrue(documentRepository.existsById(
                marioDocument.getId()));
    }

    @Test
    void preventsAnotherUserFromLinkingDocument() throws Exception {
        mockMvc.perform(put(
                "/api/documents/{documentId}/events/{eventId}",
                marioDocument.getId(),
                marioEvent.getId())
                .with(user(giulia.getEmail()))
                .with(csrf()))
                .andExpect(status().isNotFound());

        assertFalse(linkRepository.existsById(
                new MedicalEventDocumentId(
                        marioDocument.getId(),
                        marioEvent.getId())));
    }

    @Test
    void preventsLinkingAnotherUsersMedicalEvent() throws Exception {
        MedicalService giuliaService = createMedicalService(giulia);
        MedicalEvent giuliaEvent = createMedicalEvent(giulia, giuliaService);

        mockMvc.perform(put(
                "/api/documents/{documentId}/events/{eventId}",
                marioDocument.getId(),
                giuliaEvent.getId())
                .with(user(mario.getEmail()))
                .with(csrf()))
                .andExpect(status().isNotFound());

        assertFalse(linkRepository.existsById(
                new MedicalEventDocumentId(
                        marioDocument.getId(),
                        giuliaEvent.getId())));
    }

    @Test
    void returnsLinkedMedicalEvents() throws Exception {
        MedicalEvent secondEvent = createMedicalEvent(mario, marioService);

        for (Long eventId : List.of(
                marioEvent.getId(),
                secondEvent.getId())) {

            mockMvc.perform(put(
                    "/api/documents/{documentId}/events/{eventId}",
                    marioDocument.getId(),
                    eventId)
                    .with(user(mario.getEmail()))
                    .with(csrf()))
                    .andExpect(status().isNoContent());
        }

        mockMvc.perform(get(
                "/api/documents/{documentId}/events",
                marioDocument.getId())
                .with(user(mario.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0]").value(marioEvent.getId()))
                .andExpect(jsonPath("$[1]").value(secondEvent.getId()));
    }

    @Test
    void returnsDocumentsLinkedToMedicalEvent() throws Exception {
        Document secondDocument = createDocument(mario, "Secondo referto");

        for (Long documentId : List.of(
                marioDocument.getId(),
                secondDocument.getId())) {

            mockMvc.perform(put(
                    "/api/documents/{documentId}/events/{eventId}",
                    documentId,
                    marioEvent.getId())
                    .with(user(mario.getEmail()))
                    .with(csrf()))
                    .andExpect(status().isNoContent());
        }

        mockMvc.perform(get(
                "/api/medical-events/{eventId}/documents",
                marioEvent.getId())
                .with(user(mario.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0]").value(marioDocument.getId()))
                .andExpect(jsonPath("$[1]").value(secondDocument.getId()));
    }

    @Test
    void preventsAnotherUserFromReadingDocumentLinks()
            throws Exception {

        mockMvc.perform(get(
                "/api/documents/{documentId}/events",
                marioDocument.getId())
                .with(user(giulia.getEmail())))
                .andExpect(status().isNotFound());
    }

    @Test
    void preventsAnotherUserFromReadingMedicalEventLinks()
            throws Exception {

        mockMvc.perform(get(
                "/api/medical-events/{eventId}/documents",
                marioEvent.getId())
                .with(user(giulia.getEmail())))
                .andExpect(status().isNotFound());
    }

    @Test
    void deletingDocumentAutomaticallyRemovesLinks() throws Exception {
        Long documentId = marioDocument.getId();
        Long eventId = marioEvent.getId();

        mockMvc.perform(put(
                "/api/documents/{documentId}/events/{eventId}",
                documentId,
                eventId)
                .with(user(mario.getEmail()))
                .with(csrf()))
                .andExpect(status().isNoContent());

        MedicalEventDocumentId linkId = new MedicalEventDocumentId(documentId, eventId);

        assertTrue(linkRepository.existsById(linkId));

        mockMvc.perform(delete("/api/documents/{documentId}", documentId)
                .with(user(mario.getEmail()))
                .with(csrf()))
                .andExpect(status().isNoContent());

        assertFalse(linkRepository.existsById(linkId));
        assertTrue(medicalEventRepository.existsById(eventId));
    }

    @Test
    void deletingMedicalEventAutomaticallyRemovesLinks()
            throws Exception {

        Long documentId = marioDocument.getId();
        Long eventId = marioEvent.getId();

        mockMvc.perform(put(
                "/api/documents/{documentId}/events/{eventId}",
                documentId,
                eventId)
                .with(user(mario.getEmail()))
                .with(csrf()))
                .andExpect(status().isNoContent());

        MedicalEventDocumentId linkId = new MedicalEventDocumentId(documentId, eventId);

        assertTrue(linkRepository.existsById(linkId));

        // Eliminiamo la prestazione tramite repository per verificare
        // direttamente il comportamento della foreign key.
        medicalEventRepository.deleteById(eventId);
        medicalEventRepository.flush();

        assertFalse(linkRepository.existsById(linkId));
        assertTrue(documentRepository.existsById(documentId));
    }

    @Test
    void completedEventWithoutReportHasMissingReport() throws Exception {
        mockMvc.perform(get(
                "/api/medical-events/{eventId}",
                marioEvent.getId())
                .with(user(mario.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.reportMissing").value(true));
    }

    @Test
    void linkedReportDocumentRemovesMissingReport() throws Exception {
        Long eventId = marioEvent.getId();
        Long documentId = marioDocument.getId();

        mockMvc.perform(put(
                "/api/documents/{documentId}/events/{eventId}",
                documentId,
                eventId)
                .with(user(mario.getEmail()))
                .with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(
                "/api/medical-events/{eventId}",
                eventId)
                .with(user(mario.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reportMissing").value(false));
    }

    @Test
    void unlinkingOnlyReportRestoresMissingReport() throws Exception {
        Long eventId = marioEvent.getId();
        Long documentId = marioDocument.getId();

        String linkEndpoint = "/api/documents/"
                + documentId
                + "/events/"
                + eventId;

        mockMvc.perform(put(linkEndpoint)
                .with(user(mario.getEmail()))
                .with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(
                "/api/medical-events/{eventId}",
                eventId)
                .with(user(mario.getEmail())))
                .andExpect(jsonPath("$.reportMissing").value(false));

        mockMvc.perform(delete(linkEndpoint)
                .with(user(mario.getEmail()))
                .with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(
                "/api/medical-events/{eventId}",
                eventId)
                .with(user(mario.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reportMissing").value(true));
    }

    @Test
    void linkedNonReportDocumentDoesNotRemoveMissingReport()
            throws Exception {

        Document prescription = documentRepository.saveAndFlush(
                new Document(
                        mario,
                        "prescrizione.pdf",
                        "Prescrizione medica",
                        UUID.randomUUID().toString(),
                        "application/pdf",
                        1024L,
                        getCategory("Ricette"),
                        LocalDate.of(2026, 11, 10),
                        null,
                        null,
                        false));

        mockMvc.perform(put(
                "/api/documents/{documentId}/events/{eventId}",
                prescription.getId(),
                marioEvent.getId())
                .with(user(mario.getEmail()))
                .with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(
                "/api/medical-events/{eventId}",
                marioEvent.getId())
                .with(user(mario.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reportMissing").value(true));
    }

    @Test
    void textReportRemovesMissingReportWithoutDocuments()
            throws Exception {

        marioEvent.updateReportText("Controllo regolare.");
        medicalEventRepository.saveAndFlush(marioEvent);

        mockMvc.perform(get(
                "/api/medical-events/{eventId}",
                marioEvent.getId())
                .with(user(mario.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reportMissing").value(false));
    }

    @Test
    void unlinkingDocumentDoesNotInvalidateTextReport()
            throws Exception {

        marioEvent.updateReportText("Esame nella norma.");
        medicalEventRepository.saveAndFlush(marioEvent);

        String endpoint = "/api/documents/"
                + marioDocument.getId()
                + "/events/"
                + marioEvent.getId();

        mockMvc.perform(put(endpoint)
                .with(user(mario.getEmail()))
                .with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete(endpoint)
                .with(user(mario.getEmail()))
                .with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(
                "/api/medical-events/{eventId}",
                marioEvent.getId())
                .with(user(mario.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reportMissing").value(false));
    }

    @Test
    void changingDocumentFromReportToNonReportRestoresMissingReport()
            throws Exception {

        Long eventId = marioEvent.getId();

        mockMvc.perform(put(
                "/api/documents/{documentId}/events/{eventId}",
                marioDocument.getId(),
                eventId)
                .with(user(mario.getEmail()))
                .with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(
                "/api/medical-events/{eventId}",
                eventId)
                .with(user(mario.getEmail())))
                .andExpect(jsonPath("$.reportMissing").value(false));

        marioDocument.updateMetadata(
                marioDocument.getDisplayName(),
                marioDocument.getCategory(),
                marioDocument.getDocumentDate(),
                marioDocument.getExpiryDate(),
                marioDocument.getNotes(),
                false);

        documentRepository.saveAndFlush(marioDocument);

        mockMvc.perform(get(
                "/api/medical-events/{eventId}",
                eventId)
                .with(user(mario.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reportMissing").value(true));
    }

    @Test
    void removingOneOfTwoReportsDoesNotRestoreMissingReport()
            throws Exception {

        Document secondReport = createDocument(mario, "Secondo referto");

        Long eventId = marioEvent.getId();

        for (Long documentId : List.of(
                marioDocument.getId(),
                secondReport.getId())) {

            mockMvc.perform(put(
                    "/api/documents/{documentId}/events/{eventId}",
                    documentId,
                    eventId)
                    .with(user(mario.getEmail()))
                    .with(csrf()))
                    .andExpect(status().isNoContent());
        }

        mockMvc.perform(delete(
                "/api/documents/{documentId}/events/{eventId}",
                marioDocument.getId(),
                eventId)
                .with(user(mario.getEmail()))
                .with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(
                "/api/medical-events/{eventId}",
                eventId)
                .with(user(mario.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reportMissing").value(false));
    }

}
