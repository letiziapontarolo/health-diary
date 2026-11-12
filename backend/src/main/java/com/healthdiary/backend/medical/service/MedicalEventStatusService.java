
package com.healthdiary.backend.medical.service;

import com.healthdiary.backend.medical.entity.MedicalEvent;
import com.healthdiary.backend.medical.entity.MedicalEventStatus;
import com.healthdiary.backend.medical.entity.MedicalEventStatusMode;
import com.healthdiary.backend.medical.repository.MedicalEventRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class MedicalEventStatusService {

    private final MedicalEventRepository eventRepository;
    private final MedicalEventStatusCalculator statusCalculator;

    public MedicalEventStatusService(
            MedicalEventRepository eventRepository,
            MedicalEventStatusCalculator statusCalculator) {
        this.eventRepository = eventRepository;
        this.statusCalculator = statusCalculator;
    }

    @Transactional
    public int updateAutomaticStatuses() {
        List<MedicalEvent> events = eventRepository
                .findByStatusAndStatusMode(
                        MedicalEventStatus.SCHEDULED,
                        MedicalEventStatusMode.AUTO);

        int updatedCount = 0;

        for (MedicalEvent event : events) {
            MedicalEventStatus calculatedStatus = statusCalculator.calculate(
                    event.getEventDate(),
                    event.getEventTime());

            if (calculatedStatus == MedicalEventStatus.COMPLETED) {
                event.updateAutomaticStatus(calculatedStatus);
                updatedCount++;
            }
        }

        return updatedCount;
    }
}
