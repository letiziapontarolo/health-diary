package com.healthdiary.backend.document.entity;

import com.healthdiary.backend.auth.entity.User;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "document_categories")
public class DocumentCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    protected DocumentCategory() {
    }

    public DocumentCategory(User user, String name) {
        this.user = user;
        this.name = name;
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public boolean isDefault() {
        return user == null;
    }

    public void rename(String name) {
        this.name = name;
    }
}
