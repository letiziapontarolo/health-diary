package com.healthdiary.backend.medical.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "medical_event_options")
public class MedicalEventOption {

    @EmbeddedId
    private MedicalEventOptionId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("medicalEventId")
    @JoinColumn(name = "medical_event_id", nullable = false)
    private MedicalEvent medicalEvent;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("optionId")
    @JoinColumn(name = "option_id", nullable = false)
    private MedicalServiceOption option;

    @Column(name = "service_id", nullable = false)
    private Long serviceId;

    protected MedicalEventOption() {
    }

    public MedicalEventOption(
            MedicalEvent medicalEvent,
            MedicalServiceOption option) {
        this.medicalEvent = medicalEvent;
        this.option = option;
        this.serviceId = option.getService().getId();
        this.id = new MedicalEventOptionId(
                medicalEvent.getId(),
                option.getId());
    }

    public MedicalEvent getMedicalEvent() {
        return medicalEvent;
    }

    public MedicalServiceOption getOption() {
        return option;
    }
}
