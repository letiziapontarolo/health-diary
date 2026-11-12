package com.healthdiary.backend.medical.repository;

import com.healthdiary.backend.medical.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    List<Doctor> findByUserIdOrderByNameAsc(Long userId);

    Optional<Doctor> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserIdAndNameIgnoreCase(Long userId, String name);
}