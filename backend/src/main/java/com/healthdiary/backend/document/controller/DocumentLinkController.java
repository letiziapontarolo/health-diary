package com.healthdiary.backend.document.controller;

import com.healthdiary.backend.document.service.DocumentLinkService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class DocumentLinkController {

    private final DocumentLinkService documentLinkService;

    public DocumentLinkController(DocumentLinkService documentLinkService) {
        this.documentLinkService = documentLinkService;
    }

    @PutMapping("/documents/{documentId}/events/{eventId}")
    public ResponseEntity<Void> linkDocument(
            @PathVariable Long documentId,
            @PathVariable Long eventId,
            Authentication authentication) {

        documentLinkService.linkDocument(
                authentication.getName(),
                documentId,
                eventId
        );

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/documents/{documentId}/events/{eventId}")
    public ResponseEntity<Void> unlinkDocument(
            @PathVariable Long documentId,
            @PathVariable Long eventId,
            Authentication authentication) {

        documentLinkService.unlinkDocument(
                authentication.getName(),
                documentId,
                eventId
        );

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/documents/{documentId}/events")
    public List<Long> getLinkedEvents(
            @PathVariable Long documentId,
            Authentication authentication) {

        return documentLinkService.getLinkedEventIds(
                authentication.getName(),
                documentId
        );
    }

    @GetMapping("/medical-events/{eventId}/documents")
    public List<Long> getLinkedDocuments(
            @PathVariable Long eventId,
            Authentication authentication) {

        return documentLinkService.getLinkedDocumentIds(
                authentication.getName(),
                eventId
        );
    }
}
