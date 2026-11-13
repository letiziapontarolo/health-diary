package com.healthdiary.backend.document.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

@Entity
@Table(name = "medical_event_documents")
@IdClass(MedicalEventDocumentId.class)
public class MedicalEventDocument {

    @Id
    @Column(name = "document_id", nullable = false)
    private Long documentId;

    @Id
    @Column(name = "medical_event_id", nullable = false)
    private Long medicalEventId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    protected MedicalEventDocument() {
    }

    public MedicalEventDocument(
            Long documentId,
            Long medicalEventId,
            Long userId) {

        this.documentId = documentId;
        this.medicalEventId = medicalEventId;
        this.userId = userId;
    }

    public Long getDocumentId() {
        return documentId;
    }

    public Long getMedicalEventId() {
        return medicalEventId;
    }

    public Long getUserId() {
        return userId;
    }
}
