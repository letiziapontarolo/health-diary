package com.healthdiary.backend.medical.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "medical_service_options")
public class MedicalServiceOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_id", nullable = false)
    private MedicalService service;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    protected MedicalServiceOption() {
    }

    public MedicalServiceOption(MedicalService service, String name) {
        this.service = service;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public MedicalService getService() {
        return service;
    }

    public String getName() {
        return name;
    }

    public boolean isActive() {
        return active;
    }
}
