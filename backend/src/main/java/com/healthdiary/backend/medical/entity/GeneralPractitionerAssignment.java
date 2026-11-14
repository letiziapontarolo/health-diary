package com.healthdiary.backend.medical.entity;

import com.healthdiary.backend.auth.entity.User;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "user_general_practitioners")
public class GeneralPractitionerAssignment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;
    @Column(name = "start_date")
    private LocalDate startDate;
    @Column(name = "end_date")
    private LocalDate endDate;
    @Column(name = "is_current", nullable = false)
    private boolean current = true;

    protected GeneralPractitionerAssignment() {}
    public GeneralPractitionerAssignment(User user, Doctor doctor, LocalDate startDate) {
        this.user = user;
        this.doctor = doctor;
        this.startDate = startDate;
    }
    public Long getId() { return id; }
    public Doctor getDoctor() { return doctor; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public boolean isCurrent() { return current; }
    public void close(LocalDate nextStart) {
        this.current = false;
        this.endDate = nextStart == null ? null : nextStart.minusDays(1);
    }
}
