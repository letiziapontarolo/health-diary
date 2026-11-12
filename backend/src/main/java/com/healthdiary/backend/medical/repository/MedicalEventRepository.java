package com.healthdiary.backend.medical.repository;

import com.healthdiary.backend.medical.entity.MedicalEvent;
import com.healthdiary.backend.medical.entity.MedicalEventStatus;
import com.healthdiary.backend.medical.entity.MedicalEventStatusMode;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MedicalEventRepository
                extends JpaRepository<MedicalEvent, Long>,
                JpaSpecificationExecutor<MedicalEvent> {

        List<MedicalEvent> findByUserIdOrderByEventDateDescEventTimeDesc(Long userId);

        Optional<MedicalEvent> findByIdAndUserId(Long id, Long userId);

        List<MedicalEvent> findByUserIdAndStatusOrderByEventDateDesc(
                        Long userId,
                        MedicalEventStatus status);

        List<MedicalEvent> findByUserIdAndEventDateBetweenOrderByEventDateAsc(
                        Long userId,
                        LocalDate startDate,
                        LocalDate endDate);

        boolean existsByServiceId(Long serviceId);

        List<MedicalEvent> findByStatusAndStatusMode(
                        MedicalEventStatus status,
                        MedicalEventStatusMode statusMode);

        List<MedicalEvent> findByUserIdAndStatusAndStatusMode(
                        Long userId,
                        MedicalEventStatus status,
                        MedicalEventStatusMode statusMode);
}