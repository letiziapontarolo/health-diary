package com.healthdiary.backend.document.service;

import com.healthdiary.backend.auth.entity.User;
import com.healthdiary.backend.auth.repository.UserRepository;
import com.healthdiary.backend.document.dto.DocumentResponse;
import com.healthdiary.backend.document.entity.Document;
import com.healthdiary.backend.document.repository.DocumentRepository;
import com.healthdiary.backend.document.dto.UpdateDocumentRequest;
import com.healthdiary.backend.document.entity.DocumentCategory;
import com.healthdiary.backend.document.repository.DocumentCategoryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.core.io.Resource;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import com.healthdiary.backend.document.dto.CreateDocumentRequest;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import com.healthdiary.backend.document.dto.DocumentFilter;
import com.healthdiary.backend.document.repository.DocumentSpecifications;
import org.springframework.data.domain.Sort;

@Service
@Transactional
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final DocumentStorageService storageService;
    private final DocumentCategoryRepository categoryRepository;
    private final Clock clock;
    private static final Logger logger = LoggerFactory.getLogger(DocumentService.class);

    public DocumentService(
            DocumentRepository documentRepository,
            DocumentCategoryRepository categoryRepository,
            UserRepository userRepository,
            DocumentStorageService storageService,
            Clock clock) {

        this.documentRepository = documentRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.storageService = storageService;
        this.clock = clock;
    }

    private User getUser(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"));
    }

    private Document getDocument(Long documentId, Long userId) {
        return documentRepository.findByIdAndUserId(documentId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Document not found"));
    }

    private DocumentCategory getAccessibleCategory(
            Long categoryId,
            Long userId) {

        if (categoryId == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Document category is required");
        }

        return categoryRepository.findById(categoryId)
                .filter(category -> category.isDefault()
                        || category.getUser().getId().equals(userId))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Document category not found"));
    }

    private DocumentResponse toResponse(Document document) {
        return new DocumentResponse(
                document.getId(),
                document.getOriginalName(),
                document.getDisplayName(),
                document.getContentType(),
                document.getFileSize(),
                document.getCategory().getId(),
                document.getCategory().getName(),
                document.getDocumentDate(),
                document.getExpiryDate(),
                document.getNotes(),
                document.isReport(),
                document.isExpired(LocalDate.now(clock)),
                document.getCreatedAt(),
                document.getUpdatedAt());
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> getDocuments(String email) {
        User user = getUser(email);

        return documentRepository
                .findByUserIdOrderByDocumentDateDescIdDesc(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public DocumentResponse getDocumentById(Long documentId, String email) {
        User user = getUser(email);
        Document document = getDocument(documentId, user.getId());

        return toResponse(document);
    }

    @Transactional(readOnly = true)
    public Resource getDocumentFile(Long documentId, String email) {
        User user = getUser(email);
        Document document = getDocument(documentId, user.getId());

        return storageService.load(document.getStorageKey());
    }

    private void validateMetadata(String displayName) {
        if (displayName == null || displayName.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Document display name is required");
        }

        if (displayName.length() > 255) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Document display name cannot exceed 255 characters");
        }
    }

    public DocumentResponse createDocument(
            String email,
            CreateDocumentRequest request,
            MultipartFile file) {

        User user = getUser(email);

        if (request == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Document metadata is required");
        }

        validateMetadata(request.displayName());

        DocumentCategory category = getAccessibleCategory(
                request.categoryId(),
                user.getId());

        validateOriginalFilename(file);

        if (!TransactionSynchronizationManager.isSynchronizationActive()
                || !TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException(
                    "Document upload requires an active transaction");
        }

        String storageKey = storageService.store(file);
        AtomicBoolean cleanupPerformed = new AtomicBoolean(false);

        Runnable cleanup = () -> {
            if (cleanupPerformed.compareAndSet(false, true)) {
                cleanupStoredFile(storageKey);
            }
        };

        try {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCompletion(int status) {
                            if (status != TransactionSynchronization.STATUS_COMMITTED) {
                                cleanup.run();
                            }
                        }
                    });

            Document document = new Document(
                    user,
                    file.getOriginalFilename(),
                    request.displayName(),
                    storageKey,
                    file.getContentType(),
                    file.getSize(),
                    category,
                    request.documentDate(),
                    request.expiryDate(),
                    request.notes(),
                    request.report());

            document = documentRepository.saveAndFlush(document);

            return toResponse(document);

        } catch (RuntimeException exception) {
            cleanup.run();
            throw exception;
        }
    }

    private void cleanupStoredFile(String storageKey) {
        try {
            storageService.delete(storageKey);
        } catch (RuntimeException exception) {
            logger.error(
                    "Failed to clean up stored document file: {}",
                    storageKey,
                    exception);
        }
    }

    public DocumentResponse updateDocument(
            Long documentId,
            String email,
            UpdateDocumentRequest request) {

        User user = getUser(email);
        Document document = getDocument(documentId, user.getId());

        if (request == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Document metadata is required");
        }

        validateMetadata(request.displayName());

        DocumentCategory category = getAccessibleCategory(
                request.categoryId(),
                user.getId());

        document.updateMetadata(
                request.displayName(),
                category,
                request.documentDate(),
                request.expiryDate(),
                request.notes(),
                request.report());

        return toResponse(document);
    }

    public DocumentResponse replaceDocumentFile(
            Long documentId,
            String email,
            MultipartFile file) {

        User user = getUser(email);
        Document document = getDocument(documentId, user.getId());

        validateOriginalFilename(file);

        if (!TransactionSynchronizationManager.isSynchronizationActive()
                || !TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException(
                    "Document replacement requires an active transaction");
        }

        String oldStorageKey = document.getStorageKey();
        String newStorageKey = storageService.store(file);

        AtomicBoolean cleanupPerformed = new AtomicBoolean(false);

        Runnable cleanupNewFile = () -> {
            if (cleanupPerformed.compareAndSet(false, true)) {
                cleanupStoredFile(newStorageKey);
            }
        };

        try {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCompletion(int status) {
                            if (status == TransactionSynchronization.STATUS_COMMITTED) {
                                cleanupStoredFile(oldStorageKey);
                            } else {
                                cleanupNewFile.run();
                            }
                        }
                    });

            document.replaceFile(
                    file.getOriginalFilename(),
                    newStorageKey,
                    file.getContentType(),
                    file.getSize());

            documentRepository.flush();

            return toResponse(document);

        } catch (RuntimeException exception) {
            cleanupNewFile.run();
            throw exception;
        }
    }

    public void deleteDocument(Long documentId, String email) {
        User user = getUser(email);
        Document document = getDocument(documentId, user.getId());

        if (!TransactionSynchronizationManager.isSynchronizationActive()
                || !TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException(
                    "Document deletion requires an active transaction");
        }

        String storageKey = document.getStorageKey();

        documentRepository.delete(document);
        documentRepository.flush();

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        if (status == TransactionSynchronization.STATUS_COMMITTED) {
                            cleanupStoredFile(storageKey);
                        }
                    }
                });
    }

    private void validateOriginalFilename(MultipartFile file) {
        if (file == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A document file is required");
        }

        String filename = file.getOriginalFilename();

        if (filename == null
                || filename.isBlank()
                || filename.length() > 255
                || filename.contains("/")
                || filename.contains("\\")
                || filename.chars().anyMatch(Character::isISOControl)) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid original file name");
        }
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> getFilteredDocuments(
            String email,
            DocumentFilter filter) {

        User user = getUser(email);

        if (filter == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Document filter is required");
        }

        if (filter.fromDate() != null
                && filter.toDate() != null
                && filter.fromDate().isAfter(filter.toDate())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid document date range");
        }

        return documentRepository.findAll(
                DocumentSpecifications.withFilters(
                        user.getId(),
                        filter,
                        LocalDate.now(clock)),
                Sort.by(
                        Sort.Order.desc("documentDate").nullsLast(),
                        Sort.Order.desc("id")))
                .stream().map(this::toResponse).toList();
    }

}
