package com.healthdiary.backend.document.entity;

import com.healthdiary.backend.auth.entity.User;
import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "documents")
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;

    @Column(name = "display_name", nullable = false, length = 255)
    private String displayName;

    @Column(name = "storage_key", nullable = false, unique = true, length = 255)
    private String storageKey;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private DocumentCategory category;

    @Column(name = "document_date")
    private LocalDate documentDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "is_report", nullable = false)
    private boolean report = false;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private Instant updatedAt;

    protected Document() {
    }

    public Document(
            User user,
            String originalName,
            String displayName,
            String storageKey,
            String contentType,
            long fileSize,
            DocumentCategory category,
            LocalDate documentDate,
            LocalDate expiryDate,
            String notes,
            boolean report) {

        this.user = user;
        this.originalName = originalName;
        this.displayName = displayName;
        this.storageKey = storageKey;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.category = category;
        this.documentDate = documentDate;
        this.expiryDate = expiryDate;
        this.notes = notes;
        this.report = report;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getOriginalName() {
        return originalName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public String getContentType() {
        return contentType;
    }

    public long getFileSize() {
        return fileSize;
    }

    public DocumentCategory getCategory() {
        return category;
    }

    public LocalDate getDocumentDate() {
        return documentDate;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public String getNotes() {
        return notes;
    }

    public boolean isReport() {
        return report;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public boolean isExpired(LocalDate today) {
        return expiryDate != null && expiryDate.isBefore(today);
    }

    public void updateMetadata(
            String displayName,
            DocumentCategory category,
            LocalDate documentDate,
            LocalDate expiryDate,
            String notes,
            boolean report) {

        this.displayName = displayName;
        this.category = category;
        this.documentDate = documentDate;
        this.expiryDate = expiryDate;
        this.notes = notes;
        this.report = report;
    }

    public void replaceFile(
            String originalName,
            String storageKey,
            String contentType,
            long fileSize) {

        this.originalName = originalName;
        this.storageKey = storageKey;
        this.contentType = contentType;
        this.fileSize = fileSize;
    }
}
