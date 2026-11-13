package com.healthdiary.backend.document.entity;

import java.io.Serializable;
import java.util.Objects;

public class MedicalEventDocumentId implements Serializable {

    private Long documentId;
    private Long medicalEventId;

    public MedicalEventDocumentId() {
    }

    public MedicalEventDocumentId(
            Long documentId,
            Long medicalEventId) {
        this.documentId = documentId;
        this.medicalEventId = medicalEventId;
    }

    public Long getDocumentId() {
        return documentId;
    }

    public Long getMedicalEventId() {
        return medicalEventId;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof MedicalEventDocumentId other)) {
            return false;
        }

        return Objects.equals(documentId, other.documentId)
                && Objects.equals(medicalEventId, other.medicalEventId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(documentId, medicalEventId);
    }
}
