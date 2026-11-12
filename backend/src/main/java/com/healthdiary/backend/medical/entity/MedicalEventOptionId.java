package com.healthdiary.backend.medical.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class MedicalEventOptionId implements Serializable {

    @Column(name = "medical_event_id")
    private Long medicalEventId;

    @Column(name = "option_id")
    private Long optionId;

    protected MedicalEventOptionId() {
    }

    public MedicalEventOptionId(Long medicalEventId, Long optionId) {
        this.medicalEventId = medicalEventId;
        this.optionId = optionId;
    }

    public Long getMedicalEventId() {
        return medicalEventId;
    }

    public Long getOptionId() {
        return optionId;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof MedicalEventOptionId other)) {
            return false;
        }

        return Objects.equals(medicalEventId, other.medicalEventId)
                && Objects.equals(optionId, other.optionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(medicalEventId, optionId);
    }
}
