package com.healthdiary.backend.medical.repository;
import com.healthdiary.backend.medical.entity.LabMicrobiologyTarget;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface LabMicrobiologyTargetRepository extends JpaRepository<LabMicrobiologyTarget,Long> {
 List<LabMicrobiologyTarget> findByExamResultIdOrderByPositionAsc(Long resultId);
 Optional<LabMicrobiologyTarget> findByIdAndExamResultId(Long id,Long resultId);
 boolean existsByExamResultIdAndPosition(Long resultId,Integer position);
 boolean existsByExamResultIdAndPositionAndIdNot(Long resultId,Integer position,Long id);
}
