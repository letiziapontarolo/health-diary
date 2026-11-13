package com.healthdiary.backend.medical.repository;

import com.healthdiary.backend.medical.entity.ExamResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExamResultRepository
        extends JpaRepository<ExamResult, Long> {

    List<ExamResult> findByExamIdOrderByPositionAsc(Long examId);

    Optional<ExamResult> findByIdAndExamId(
            Long id,
            Long examId);

    boolean existsByExamIdAndPosition(
            Long examId,
            Integer position);

    List<ExamResult> findByParameterIdAndExamUserIdOrderByExamEventDateAsc(
            Long parameterId,
            Long userId);
}
