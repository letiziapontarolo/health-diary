package com.healthdiary.backend.document.service;

import com.healthdiary.backend.auth.entity.User;
import com.healthdiary.backend.auth.repository.UserRepository;
import com.healthdiary.backend.document.entity.MedicalEventDocument;
import com.healthdiary.backend.document.entity.MedicalEventDocumentId;
import com.healthdiary.backend.document.repository.DocumentRepository;
import com.healthdiary.backend.document.repository.MedicalEventDocumentRepository;
import com.healthdiary.backend.medical.repository.MedicalEventRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional
public class DocumentLinkService {

    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final MedicalEventRepository medicalEventRepository;
    private final MedicalEventDocumentRepository linkRepository;

    public DocumentLinkService(
            UserRepository userRepository,
            DocumentRepository documentRepository,
            MedicalEventRepository medicalEventRepository,
            MedicalEventDocumentRepository linkRepository) {

        this.userRepository = userRepository;
        this.documentRepository = documentRepository;
        this.medicalEventRepository = medicalEventRepository;
        this.linkRepository = linkRepository;
    }

    private User getUser(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));
    }

    private void validateDocument(Long documentId, Long userId) {
        if (documentRepository.findByIdAndUserId(
                documentId, userId).isEmpty()) {

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Document not found"
            );
        }
    }

    private void validateMedicalEvent(Long eventId, Long userId) {
        if (medicalEventRepository.findByIdAndUserId(
                eventId, userId).isEmpty()) {

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Medical event not found"
            );
        }
    }

    public void linkDocument(
            String email,
            Long documentId,
            Long eventId) {

        User user = getUser(email);
        Long userId = user.getId();

        validateDocument(documentId, userId);
        validateMedicalEvent(eventId, userId);

        MedicalEventDocumentId linkId =
                new MedicalEventDocumentId(documentId, eventId);

        if (linkRepository.existsById(linkId)) {
            return;
        }

        linkRepository.saveAndFlush(new MedicalEventDocument(
                documentId,
                eventId,
                userId
        ));
    }

    public void unlinkDocument(
            String email,
            Long documentId,
            Long eventId) {

        User user = getUser(email);
        Long userId = user.getId();

        validateDocument(documentId, userId);
        validateMedicalEvent(eventId, userId);

        MedicalEventDocumentId linkId =
                new MedicalEventDocumentId(documentId, eventId);

        if (!linkRepository.existsById(linkId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Document link not found"
            );
        }

        linkRepository.deleteById(linkId);
    }

    @Transactional(readOnly = true)
    public List<Long> getLinkedEventIds(
            String email,
            Long documentId) {

        User user = getUser(email);
        validateDocument(documentId, user.getId());

        return linkRepository
                .findByDocumentIdAndUserId(
                        documentId,
                        user.getId()
                )
                .stream()
                .map(MedicalEventDocument::getMedicalEventId)
                .sorted()
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Long> getLinkedDocumentIds(
            String email,
            Long eventId) {

        User user = getUser(email);
        validateMedicalEvent(eventId, user.getId());

        return linkRepository
                .findByMedicalEventIdAndUserId(
                        eventId,
                        user.getId()
                )
                .stream()
                .map(MedicalEventDocument::getDocumentId)
                .sorted()
                .toList();
    }
}
