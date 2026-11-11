package com.healthdiary.backend.medical.repository;

import com.healthdiary.backend.medical.entity.MedicalServiceOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MedicalServiceOptionRepository
        extends JpaRepository<MedicalServiceOption, Long> {

    List<MedicalServiceOption> findByServiceIdAndActiveTrueOrderByNameAsc(
            Long serviceId
    );

    List<MedicalServiceOption> findByServiceIdOrderByNameAsc(
            Long serviceId
    );
}
