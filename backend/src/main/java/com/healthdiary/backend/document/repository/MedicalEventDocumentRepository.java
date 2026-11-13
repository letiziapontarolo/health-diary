package com.healthdiary.backend.document.repository;

import com.healthdiary.backend.document.entity.MedicalEventDocument;
import com.healthdiary.backend.document.entity.MedicalEventDocumentId;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface MedicalEventDocumentRepository
        extends JpaRepository<MedicalEventDocument, MedicalEventDocumentId> {

    List<MedicalEventDocument> findByDocumentIdAndUserId(
            Long documentId,
            Long userId);

    List<MedicalEventDocument> findByMedicalEventIdAndUserId(
            Long medicalEventId,
            Long userId);

    boolean existsByDocumentIdAndMedicalEventIdAndUserId(
            Long documentId,
            Long medicalEventId,
            Long userId);

    @Query("""
            SELECT CASE WHEN COUNT(d) > 0 THEN true ELSE false END
            FROM Document d, MedicalEventDocument link
            WHERE link.documentId = d.id
              AND link.medicalEventId = :eventId
              AND link.userId = :userId
              AND d.user.id = :userId
              AND d.report = true
            """)
    boolean existsReportForMedicalEvent(
            @Param("eventId") Long eventId,
            @Param("userId") Long userId);

}
