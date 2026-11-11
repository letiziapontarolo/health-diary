package com.healthdiary.backend.medical.repository;

import com.healthdiary.backend.medical.entity.MedicalService;
import com.healthdiary.backend.medical.entity.MedicalServiceCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

import java.util.List;

public interface MedicalServiceRepository
        extends JpaRepository<MedicalService, Long> {

    List<MedicalService> findByUserIsNullAndActiveTrueOrderByNameAsc();

    List<MedicalService> findByUserIdAndActiveTrueOrderByNameAsc(Long userId);

    List<MedicalService> findByUserIsNullAndCategoryAndActiveTrueOrderByNameAsc(
            MedicalServiceCategory category);

    List<MedicalService> findByUserIdAndCategoryAndActiveTrueOrderByNameAsc(
            Long userId,
            MedicalServiceCategory category);

    List<MedicalService> findByUserIdOrderByNameAsc(Long userId);

    List<MedicalService> findByUserIdAndActiveFalseOrderByNameAsc(Long userId);

    Optional<MedicalService> findByIdAndUserIsNullAndActiveTrue(Long id);

    boolean existsByUserIdAndNameIgnoreCaseAndCategory(
            Long userId,
            String name,
            MedicalServiceCategory category);

    Optional<MedicalService> findByIdAndUserId(
            Long id,
            Long userId);
}