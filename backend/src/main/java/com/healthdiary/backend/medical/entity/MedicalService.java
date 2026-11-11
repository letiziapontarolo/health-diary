package com.healthdiary.backend.medical.entity;

import com.healthdiary.backend.auth.entity.User;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "medical_services")
public class MedicalService {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 20)
    private MedicalServiceCategory category;

    @Column(name = "icon_key", length = 100)
    private String iconKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "option_selection_mode", nullable = false, length = 10)
    private OptionSelectionMode optionSelectionMode = OptionSelectionMode.MULTIPLE;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false)
    private Instant updatedAt;

    protected MedicalService() {
    }

    public MedicalService(
            User user,
            String name,
            MedicalServiceCategory category,
            String iconKey) {
        this.user = user;
        this.name = name;
        this.category = category;
        this.iconKey = iconKey;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getName() {
        return name;
    }

    public MedicalServiceCategory getCategory() {
        return category;
    }

    public String getIconKey() {
        return iconKey;
    }

    public OptionSelectionMode getOptionSelectionMode() {
        return optionSelectionMode;
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
