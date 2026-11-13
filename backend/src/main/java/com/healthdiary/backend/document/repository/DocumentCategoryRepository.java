package com.healthdiary.backend.document.repository;

import com.healthdiary.backend.document.entity.DocumentCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface DocumentCategoryRepository
        extends JpaRepository<DocumentCategory, Long> {

    List<DocumentCategory> findByUserIsNullOrUserIdOrderByNameAsc(
            Long userId);

    Optional<DocumentCategory> findByIdAndUserIsNullOrIdAndUserId(
            Long globalCategoryId,
            Long userCategoryId,
            Long userId);

    Optional<DocumentCategory> findByIdAndUserId(
            Long categoryId,
            Long userId);

    @Query("""
            SELECT COUNT(c) > 0
            FROM DocumentCategory c
            WHERE LOWER(TRIM(c.name)) = LOWER(TRIM(:name))
              AND (c.user IS NULL OR c.user.id = :userId)
            """)
    boolean existsAccessibleCategoryWithName(
            @Param("name") String name,
            @Param("userId") Long userId);

    @Query("""
            SELECT COUNT(c) > 0
            FROM DocumentCategory c
            WHERE LOWER(TRIM(c.name)) = LOWER(TRIM(:name))
              AND (c.user IS NULL OR c.user.id = :userId)
              AND c.id <> :excludedId
            """)
    boolean existsAccessibleCategoryWithNameExcludingId(
            @Param("name") String name,
            @Param("userId") Long userId,
            @Param("excludedId") Long excludedId);

}
