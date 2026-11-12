package com.healthdiary.backend.medical.service;

import com.healthdiary.backend.auth.entity.User;
import com.healthdiary.backend.auth.repository.UserRepository;
import com.healthdiary.backend.medical.dto.*;
import com.healthdiary.backend.medical.entity.Doctor;
import com.healthdiary.backend.medical.entity.Facility;
import com.healthdiary.backend.medical.repository.DoctorRepository;
import com.healthdiary.backend.medical.repository.FacilityRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional
public class MedicalProviderService {

    private final DoctorRepository doctorRepository;
    private final FacilityRepository facilityRepository;
    private final UserRepository userRepository;

    public MedicalProviderService(
            DoctorRepository doctorRepository,
            FacilityRepository facilityRepository,
            UserRepository userRepository
    ) {
        this.doctorRepository = doctorRepository;
        this.facilityRepository = facilityRepository;
        this.userRepository = userRepository;
    }

    private User getUser(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));
    }

    public DoctorResponse createDoctor(
            String email,
            CreateDoctorRequest request
    ) {
        User user = getUser(email);
        String name = request.name().trim();

        if (doctorRepository.existsByUserIdAndNameIgnoreCase(
                user.getId(), name)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Doctor already exists"
            );
        }

        Doctor doctor = new Doctor(
                user,
                name,
                normalize(request.specialization())
        );

        return toDoctorResponse(doctorRepository.save(doctor));
    }

    @Transactional(readOnly = true)
    public List<DoctorResponse> getDoctors(String email) {
        User user = getUser(email);

        return doctorRepository
                .findByUserIdOrderByNameAsc(user.getId())
                .stream()
                .map(this::toDoctorResponse)
                .toList();
    }

    public FacilityResponse createFacility(
            String email,
            CreateFacilityRequest request
    ) {
        User user = getUser(email);
        String name = request.name().trim();

        if (facilityRepository.existsByUserIdAndNameIgnoreCase(
                user.getId(), name)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Facility already exists"
            );
        }

        Facility facility = new Facility(
                user,
                name,
                normalize(request.city())
        );

        return toFacilityResponse(facilityRepository.save(facility));
    }

    @Transactional(readOnly = true)
    public List<FacilityResponse> getFacilities(String email) {
        User user = getUser(email);

        return facilityRepository
                .findByUserIdOrderByNameAsc(user.getId())
                .stream()
                .map(this::toFacilityResponse)
                .toList();
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private DoctorResponse toDoctorResponse(Doctor doctor) {
        return new DoctorResponse(
                doctor.getId(),
                doctor.getName(),
                doctor.getSpecialization()
        );
    }

    private FacilityResponse toFacilityResponse(Facility facility) {
        return new FacilityResponse(
                facility.getId(),
                facility.getName(),
                facility.getCity()
        );
    }
}
