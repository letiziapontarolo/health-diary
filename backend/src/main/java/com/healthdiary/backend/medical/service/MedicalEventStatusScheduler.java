package com.healthdiary.backend.medical.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MedicalEventStatusScheduler {

    private final MedicalEventStatusService statusService;

    public MedicalEventStatusScheduler(
            MedicalEventStatusService statusService
    ) {
        this.statusService = statusService;
    }

    @Scheduled(fixedDelay = 60_000)
    public void updateStatuses() {
        statusService.updateAutomaticStatuses();
    }
}
