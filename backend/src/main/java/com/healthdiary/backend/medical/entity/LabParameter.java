package com.healthdiary.backend.medical.entity;

import com.healthdiary.backend.auth.entity.User;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "lab_parameters")
public class LabParameter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_id", nullable = false)
    private MedicalService service;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "group_key", length = 50)
    private String groupKey;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false,
            insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false,
            insertable = false)
    private Instant updatedAt;

    protected LabParameter() {
    }

    public LabParameter(
            MedicalService service,
            User user,
            String name,
            String groupKey) {

        this.service = service;
        this.user = user;
        this.name = name;
        this.groupKey = groupKey;
    }

    public Long getId() {
        return id;
    }

    public MedicalService getService() {
        return service;
    }

    public User getUser() {
        return user;
    }

    public String getName() {
        return name;
    }

    public String getGroupKey() {
        return groupKey;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void rename(String name) {
        this.name = name;
    }

    public void archive() {
        this.active = false;
    }

    public void restore() {
        this.active = true;
    }
}
