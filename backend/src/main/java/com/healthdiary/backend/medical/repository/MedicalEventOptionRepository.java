package com.healthdiary.backend.medical.repository;

import com.healthdiary.backend.medical.entity.MedicalEventOption;
import com.healthdiary.backend.medical.entity.MedicalEventOptionId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MedicalEventOptionRepository
        extends JpaRepository<MedicalEventOption, MedicalEventOptionId> {

    List<MedicalEventOption> findByMedicalEventIdOrderByOptionNameAsc(
            Long medicalEventId
    );

    void deleteByMedicalEventId(Long medicalEventId);

    boolean existsByMedicalEventId(Long medicalEventId);
}
