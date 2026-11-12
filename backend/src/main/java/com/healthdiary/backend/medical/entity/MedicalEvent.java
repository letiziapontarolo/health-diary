package com.healthdiary.backend.medical.entity;

import com.healthdiary.backend.auth.entity.User;
import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "medical_events")
public class MedicalEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_id", nullable = false)
    private MedicalService service;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id")
    private Doctor doctor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facility_id")
    private Facility facility;

    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;

    @Column(name = "event_time")
    private LocalTime eventTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MedicalEventStatus status = MedicalEventStatus.SCHEDULED;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_mode", nullable = false, length = 10)
    private MedicalEventStatusMode statusMode = MedicalEventStatusMode.AUTO;

    @Column(name = "report_text", columnDefinition = "TEXT")
    private String reportText;

    @Column(name = "report_not_expected", nullable = false)
    private boolean reportNotExpected = false;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private Instant updatedAt;

    protected MedicalEvent() {
    }

    public MedicalEvent(
            User user,
            MedicalService service,
            LocalDate eventDate,
            LocalTime eventTime,
            Doctor doctor,
            Facility facility,
            String notes) {
        this.user = user;
        this.service = service;
        this.eventDate = eventDate;
        this.eventTime = eventTime;
        this.doctor = doctor;
        this.facility = facility;
        this.notes = notes;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public MedicalService getService() {
        return service;
    }

    public Doctor getDoctor() {
        return doctor;
    }

    public Facility getFacility() {
        return facility;
    }

    public LocalDate getEventDate() {
        return eventDate;
    }

    public LocalTime getEventTime() {
        return eventTime;
    }

    public MedicalEventStatus getStatus() {
        return status;
    }

    public MedicalEventStatusMode getStatusMode() {
        return statusMode;
    }

    public String getReportText() {
        return reportText;
    }

    public boolean isReportNotExpected() {
        return reportNotExpected;
    }

    public boolean isReportMissing() {
        return this.status == MedicalEventStatus.COMPLETED
                && (this.reportText == null || this.reportText.isBlank())
                && !this.reportNotExpected;
    }

    public String getNotes() {
        return notes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void update(
            MedicalService service,
            LocalDate eventDate,
            LocalTime eventTime,
            Doctor doctor,
            Facility facility,
            String notes) {
        this.service = service;
        this.eventDate = eventDate;
        this.eventTime = eventTime;
        this.doctor = doctor;
        this.facility = facility;
        this.notes = notes;
    }

    public void cancel() {
        this.status = MedicalEventStatus.CANCELLED;
        this.statusMode = MedicalEventStatusMode.MANUAL;
    }

    public void restore(MedicalEventStatus restoredStatus) {
        this.status = restoredStatus;
        this.statusMode = MedicalEventStatusMode.AUTO;
    }

    public void completeAutomatically() {
        if (this.status == MedicalEventStatus.SCHEDULED
                && this.statusMode == MedicalEventStatusMode.AUTO) {
            this.status = MedicalEventStatus.COMPLETED;
        }
    }

    public void updateAutomaticStatus(MedicalEventStatus newStatus) {
        if (this.status != MedicalEventStatus.CANCELLED
                && this.statusMode == MedicalEventStatusMode.AUTO) {
            this.status = newStatus;
        }
    }

    public void updateReportText(String reportText) {
        if (reportText == null || reportText.isBlank()) {
            throw new IllegalArgumentException(
                    "Report text cannot be empty");
        }

        this.reportText = reportText.trim();
        this.reportNotExpected = false;
    }

    public void deleteReportText() {
        this.reportText = null;
        this.reportNotExpected = false;
    }

    public void setReportNotExpected(boolean reportNotExpected) {
        this.reportNotExpected = reportNotExpected;

        if (reportNotExpected) {
            this.reportText = null;
        }
    }

}
