package com.healthdiary.backend.medical.service;

import com.healthdiary.backend.auth.entity.User;
import com.healthdiary.backend.auth.repository.UserRepository;
import com.healthdiary.backend.medical.dto.*;
import com.healthdiary.backend.medical.entity.*;
import com.healthdiary.backend.medical.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.util.List;

@Service
public class GeneralPractitionerService {
    private final EntityManager entityManager;
    private final UserRepository users;
    private final DoctorRepository doctors;
    private final GeneralPractitionerAssignmentRepository assignments;

    public GeneralPractitionerService(UserRepository users, DoctorRepository doctors,
            GeneralPractitionerAssignmentRepository assignments, EntityManager entityManager) {
        this.entityManager = entityManager;
        this.users = users;
        this.doctors = doctors;
        this.assignments = assignments;
    }

    private User user(String email) {
        return users.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private GeneralPractitionerResponse response(GeneralPractitionerAssignment a) {
        return new GeneralPractitionerResponse(a.getId(), a.getDoctor().getId(),
                a.getDoctor().getName(), a.getStartDate(), a.getEndDate(), a.isCurrent());
    }

    @Transactional(readOnly = true)
    public GeneralPractitionerResponse current(String email) {
        return assignments.findByUserIdAndCurrentTrue(user(email).getId())
                .map(this::response).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<GeneralPractitionerResponse> history(String email) {
        return assignments.findByUserIdOrderByIdDesc(user(email).getId())
                .stream().map(this::response).toList();
    }

    @Transactional
    public GeneralPractitionerResponse change(String email, SetGeneralPractitionerRequest request) {
        User user = user(email);
        // Serialize concurrent changes for the same user; the partial unique index is a
        // second safeguard.
        entityManager.lock(user, LockModeType.PESSIMISTIC_WRITE);
        Doctor doctor = doctors.findByIdAndUserId(request.doctorId(), user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Doctor not found"));
        var previous = assignments.findByUserIdAndCurrentTrue(user.getId());
        if (previous.isPresent()) {
            var old = previous.get();
            if (old.getDoctor().getId().equals(doctor.getId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Doctor is already current");
            }
            LocalDate start = request.startDate();
            if (start != null && old.getStartDate() != null && !start.isAfter(old.getStartDate())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "New start date must be after previous start date");
            }
            old.close(start);
            assignments.saveAndFlush(old);
        }
        return response(assignments.save(new GeneralPractitionerAssignment(user, doctor, request.startDate())));
    }
}
