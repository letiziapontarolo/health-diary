package com.healthdiary.backend.medical.repository;

import com.healthdiary.backend.medical.entity.Facility;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FacilityRepository extends JpaRepository<Facility, Long> {

    List<Facility> findByUserIdOrderByNameAsc(Long userId);

    Optional<Facility> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserIdAndNameIgnoreCase(Long userId, String name);
}