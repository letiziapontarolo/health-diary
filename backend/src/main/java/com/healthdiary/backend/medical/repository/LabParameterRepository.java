package com.healthdiary.backend.medical.repository;

import com.healthdiary.backend.medical.entity.LabParameter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LabParameterRepository
        extends JpaRepository<LabParameter, Long> {

    List<LabParameter> findByServiceIdAndUserIsNullAndActiveTrueOrderByNameAsc(
            Long serviceId);

    List<LabParameter> findByServiceIdAndUserIdAndActiveTrueOrderByNameAsc(
            Long serviceId,
            Long userId);

    List<LabParameter> findByUserIdOrderByNameAsc(Long userId);

    List<LabParameter> findByUserIdAndActiveFalseOrderByNameAsc(
            Long userId);

    Optional<LabParameter> findByIdAndUserId(
            Long id,
            Long userId);

    boolean existsByServiceIdAndUserIdAndNameIgnoreCase(
            Long serviceId,
            Long userId,
            String name);
}
