package com.healthdiary.backend.document.controller;

import com.healthdiary.backend.document.dto.CreateDocumentRequest;
import com.healthdiary.backend.document.dto.DocumentResponse;
import com.healthdiary.backend.document.dto.UpdateDocumentRequest;
import com.healthdiary.backend.document.service.DocumentService;

import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import com.healthdiary.backend.document.dto.DocumentFilter;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping
    public List<DocumentResponse> getDocuments(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String notes,
            @RequestParam(required = false) Boolean expiredOnly,
            Authentication authentication) {

        DocumentFilter filter = new DocumentFilter(
                name,
                categoryId,
                fromDate,
                toDate,
                notes,
                expiredOnly);

        return documentService.getFilteredDocuments(
                authentication.getName(),
                filter);
    }

    @GetMapping("/{documentId}")
    public DocumentResponse getDocumentById(
            @PathVariable Long documentId,
            Authentication authentication) {

        return documentService.getDocumentById(
                documentId,
                authentication.getName());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentResponse> createDocument(
            @RequestPart("metadata") CreateDocumentRequest metadata,
            @RequestPart("file") MultipartFile file,
            Authentication authentication) {

        DocumentResponse response = documentService.createDocument(
                authentication.getName(),
                metadata,
                file);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PutMapping("/{documentId}")
    public DocumentResponse updateDocument(
            @PathVariable Long documentId,
            @RequestBody UpdateDocumentRequest request,
            Authentication authentication) {

        return documentService.updateDocument(
                documentId,
                authentication.getName(),
                request);
    }

    @PutMapping(value = "/{documentId}/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DocumentResponse replaceDocumentFile(
            @PathVariable Long documentId,
            @RequestPart("file") MultipartFile file,
            Authentication authentication) {

        return documentService.replaceDocumentFile(
                documentId,
                authentication.getName(),
                file);
    }

    @GetMapping("/{documentId}/file")
    public ResponseEntity<Resource> downloadDocument(
            @PathVariable Long documentId,
            Authentication authentication) {

        DocumentResponse document = documentService.getDocumentById(
                documentId,
                authentication.getName());

        Resource resource = documentService.getDocumentFile(
                documentId,
                authentication.getName());

        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(document.originalName(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(document.contentType()))
                .contentLength(document.fileSize())
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        disposition.toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(resource);
    }

    @DeleteMapping("/{documentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDocument(
            @PathVariable Long documentId,
            Authentication authentication) {

        documentService.deleteDocument(
                documentId,
                authentication.getName());
    }
}
