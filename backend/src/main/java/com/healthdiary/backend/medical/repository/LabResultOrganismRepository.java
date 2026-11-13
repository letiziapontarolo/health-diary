package com.healthdiary.backend.medical.repository;
import com.healthdiary.backend.medical.entity.LabResultOrganism;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface LabResultOrganismRepository extends JpaRepository<LabResultOrganism,Long> {
 List<LabResultOrganism> findByExamResultIdOrderByPositionAsc(Long resultId);
 Optional<LabResultOrganism> findByIdAndExamResultId(Long id,Long resultId);
 boolean existsByExamResultIdAndPosition(Long resultId,Integer position);
 boolean existsByExamResultIdAndPositionAndIdNot(Long resultId,Integer position,Long id);
}
