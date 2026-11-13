package com.healthdiary.backend.document.service;

import com.healthdiary.backend.auth.entity.User;
import com.healthdiary.backend.auth.repository.UserRepository;
import com.healthdiary.backend.document.entity.MedicalEventDocument;
import com.healthdiary.backend.document.entity.MedicalEventDocumentId;
import com.healthdiary.backend.document.repository.DocumentRepository;
import com.healthdiary.backend.document.repository.MedicalEventDocumentRepository;
import com.healthdiary.backend.medical.repository.MedicalEventRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentLinkServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private MedicalEventRepository medicalEventRepository;

    @Mock
    private MedicalEventDocumentRepository linkRepository;

    @InjectMocks
    private DocumentLinkService service;

    private static final String EMAIL = "mario@test.com";
    private static final Long USER_ID = 1L;
    private static final Long DOCUMENT_ID = 10L;
    private static final Long EVENT_ID = 20L;

    @BeforeEach
    void setUp() {
        User user = new User(
                "Mario",
                "Rossi",
                EMAIL,
                "password-hash",
                LocalDate.of(1995, 5, 10));

        ReflectionTestUtils.setField(user, "id", USER_ID);

        lenient().when(userRepository.findByEmailIgnoreCase(EMAIL))
                .thenReturn(Optional.of(user));
    }

    @Test
    void linksDocumentToMedicalEvent() {
        when(documentRepository.findByIdAndUserId(
                DOCUMENT_ID, USER_ID))
                .thenReturn(Optional.of(mock(
                        com.healthdiary.backend.document.entity.Document.class)));

        when(medicalEventRepository.findByIdAndUserId(
                EVENT_ID, USER_ID))
                .thenReturn(Optional.of(mock(
                        com.healthdiary.backend.medical.entity.MedicalEvent.class)));

        service.linkDocument(EMAIL, DOCUMENT_ID, EVENT_ID);

        verify(linkRepository).saveAndFlush(argThat(link -> link.getDocumentId().equals(DOCUMENT_ID)
                && link.getMedicalEventId().equals(EVENT_ID)
                && link.getUserId().equals(USER_ID)));
    }

    @Test
    void doesNotCreateDuplicateLinks() {
        when(documentRepository.findByIdAndUserId(
                DOCUMENT_ID, USER_ID))
                .thenReturn(Optional.of(mock(
                        com.healthdiary.backend.document.entity.Document.class)));

        when(medicalEventRepository.findByIdAndUserId(
                EVENT_ID, USER_ID))
                .thenReturn(Optional.of(mock(
                        com.healthdiary.backend.medical.entity.MedicalEvent.class)));

        when(linkRepository.existsById(
                new MedicalEventDocumentId(DOCUMENT_ID, EVENT_ID)))
                .thenReturn(true);

        service.linkDocument(EMAIL, DOCUMENT_ID, EVENT_ID);

        verify(linkRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsLinkWhenDocumentDoesNotBelongToUser() {
        when(documentRepository.findByIdAndUserId(
                DOCUMENT_ID, USER_ID))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.linkDocument(
                        EMAIL,
                        DOCUMENT_ID,
                        EVENT_ID));

        assertEquals(404, exception.getStatusCode().value());

        verifyNoInteractions(
                medicalEventRepository,
                linkRepository);
    }

    @Test
    void unlinksDocumentFromMedicalEvent() {
        when(documentRepository.findByIdAndUserId(
                DOCUMENT_ID, USER_ID))
                .thenReturn(Optional.of(mock(
                        com.healthdiary.backend.document.entity.Document.class)));

        when(medicalEventRepository.findByIdAndUserId(
                EVENT_ID, USER_ID))
                .thenReturn(Optional.of(mock(
                        com.healthdiary.backend.medical.entity.MedicalEvent.class)));

        MedicalEventDocumentId linkId = new MedicalEventDocumentId(DOCUMENT_ID, EVENT_ID);

        when(linkRepository.existsById(linkId)).thenReturn(true);

        service.unlinkDocument(EMAIL, DOCUMENT_ID, EVENT_ID);

        verify(linkRepository).deleteById(linkId);

        // Il documento deve rimanere nell'archivio.
        verify(documentRepository, never()).delete(
                any(com.healthdiary.backend.document.entity.Document.class));
        verify(documentRepository, never()).deleteById(anyLong());
    }

    @Test
    void rejectsUnlinkWhenLinkDoesNotExist() {
        when(documentRepository.findByIdAndUserId(
                DOCUMENT_ID, USER_ID))
                .thenReturn(Optional.of(mock(
                        com.healthdiary.backend.document.entity.Document.class)));

        when(medicalEventRepository.findByIdAndUserId(
                EVENT_ID, USER_ID))
                .thenReturn(Optional.of(mock(
                        com.healthdiary.backend.medical.entity.MedicalEvent.class)));

        when(linkRepository.existsById(
                new MedicalEventDocumentId(DOCUMENT_ID, EVENT_ID)))
                .thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.unlinkDocument(
                        EMAIL, DOCUMENT_ID, EVENT_ID));

        assertEquals(404, exception.getStatusCode().value());

        verify(linkRepository, never()).deleteById(any());
    }

    @Test
    void rejectsLinkWhenMedicalEventDoesNotBelongToUser() {
        when(documentRepository.findByIdAndUserId(
                DOCUMENT_ID, USER_ID))
                .thenReturn(Optional.of(mock(
                        com.healthdiary.backend.document.entity.Document.class)));

        when(medicalEventRepository.findByIdAndUserId(
                EVENT_ID, USER_ID))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.linkDocument(
                        EMAIL, DOCUMENT_ID, EVENT_ID));

        assertEquals(404, exception.getStatusCode().value());

        verifyNoInteractions(linkRepository);
    }

    @Test
    void returnsMedicalEventsLinkedToDocument() {
        when(documentRepository.findByIdAndUserId(
                DOCUMENT_ID, USER_ID))
                .thenReturn(Optional.of(mock(
                        com.healthdiary.backend.document.entity.Document.class)));

        when(linkRepository.findByDocumentIdAndUserId(
                DOCUMENT_ID, USER_ID))
                .thenReturn(List.of(
                        new MedicalEventDocument(DOCUMENT_ID, 30L, USER_ID),
                        new MedicalEventDocument(DOCUMENT_ID, 20L, USER_ID)));

        List<Long> eventIds = service.getLinkedEventIds(EMAIL, DOCUMENT_ID);

        assertEquals(List.of(20L, 30L), eventIds);
    }

    @Test
    void returnsDocumentsLinkedToMedicalEvent() {
        when(medicalEventRepository.findByIdAndUserId(
                EVENT_ID, USER_ID))
                .thenReturn(Optional.of(mock(
                        com.healthdiary.backend.medical.entity.MedicalEvent.class)));

        when(linkRepository.findByMedicalEventIdAndUserId(
                EVENT_ID, USER_ID))
                .thenReturn(List.of(
                        new MedicalEventDocument(40L, EVENT_ID, USER_ID),
                        new MedicalEventDocument(10L, EVENT_ID, USER_ID)));

        List<Long> documentIds = service.getLinkedDocumentIds(EMAIL, EVENT_ID);

        assertEquals(List.of(10L, 40L), documentIds);
    }

}
