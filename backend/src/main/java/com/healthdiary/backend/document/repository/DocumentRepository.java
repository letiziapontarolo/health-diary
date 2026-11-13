package com.healthdiary.backend.document.repository;

import com.healthdiary.backend.document.entity.Document;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface DocumentRepository
        extends JpaRepository<Document, Long>,
                JpaSpecificationExecutor<Document> {

    List<Document> findByUserIdOrderByDocumentDateDescIdDesc(Long userId);

    Optional<Document> findByIdAndUserId(Long id, Long userId);

    boolean existsByCategoryId(Long categoryId);

}
