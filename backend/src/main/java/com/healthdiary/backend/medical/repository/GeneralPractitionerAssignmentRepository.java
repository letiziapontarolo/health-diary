package com.healthdiary.backend.medical.repository;

import com.healthdiary.backend.medical.entity.GeneralPractitionerAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface GeneralPractitionerAssignmentRepository extends JpaRepository<GeneralPractitionerAssignment, Long> {
    Optional<GeneralPractitionerAssignment> findByUserIdAndCurrentTrue(Long userId);
    List<GeneralPractitionerAssignment> findByUserIdOrderByIdDesc(Long userId);
}
