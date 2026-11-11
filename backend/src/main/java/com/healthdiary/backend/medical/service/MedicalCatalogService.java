package com.healthdiary.backend.medical.service;

import com.healthdiary.backend.medical.dto.MedicalServiceResponse;
import com.healthdiary.backend.medical.entity.MedicalService;
import com.healthdiary.backend.medical.repository.MedicalServiceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.healthdiary.backend.medical.entity.MedicalServiceCategory;
import com.healthdiary.backend.medical.dto.MedicalServiceOptionResponse;
import com.healthdiary.backend.medical.repository.MedicalServiceOptionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.healthdiary.backend.auth.entity.User;
import com.healthdiary.backend.auth.repository.UserRepository;
import com.healthdiary.backend.medical.dto.UpdateMedicalServiceRequest;
import com.healthdiary.backend.medical.dto.CreateMedicalServiceRequest;

import java.util.List;

@Service
public class MedicalCatalogService {

    private final MedicalServiceRepository medicalServiceRepository;
    private final MedicalServiceOptionRepository medicalServiceOptionRepository;
    private final UserRepository userRepository;

    public MedicalCatalogService(
            MedicalServiceRepository medicalServiceRepository,
            MedicalServiceOptionRepository medicalServiceOptionRepository,
            UserRepository userRepository) {

        this.medicalServiceRepository = medicalServiceRepository;
        this.medicalServiceOptionRepository = medicalServiceOptionRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<MedicalServiceResponse> getDefaultServices() {
        return medicalServiceRepository
                .findByUserIsNullAndActiveTrueOrderByNameAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private MedicalServiceResponse toResponse(MedicalService service) {
        return new MedicalServiceResponse(
                service.getId(),
                service.getName(),
                service.getCategory(),
                service.getIconKey(),
                service.getOptionSelectionMode());
    }

    @Transactional(readOnly = true)
    public List<MedicalServiceResponse> getDefaultServicesByCategory(
            MedicalServiceCategory category) {

        return medicalServiceRepository
                .findByUserIsNullAndCategoryAndActiveTrueOrderByNameAsc(category)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MedicalServiceOptionResponse> getOptions(Long serviceId) {

        medicalServiceRepository
                .findByIdAndUserIsNullAndActiveTrue(serviceId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Medical service not found"));

        return medicalServiceOptionRepository
                .findByServiceIdAndActiveTrueOrderByNameAsc(serviceId)
                .stream()
                .map(option -> new MedicalServiceOptionResponse(
                        option.getId(),
                        option.getName()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MedicalServiceResponse> getUserServices(String email) {

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"));

        return medicalServiceRepository
                .findByUserIdAndActiveTrueOrderByNameAsc(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public MedicalServiceResponse createService(
            String email,
            CreateMedicalServiceRequest request) {

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"));

        String name = request.name().trim();

        if (medicalServiceRepository
                .existsByUserIdAndNameIgnoreCaseAndCategory(
                        user.getId(),
                        name,
                        request.category())) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Medical service already exists");
        }

        MedicalService service = new MedicalService(
                user,
                name,
                request.category(),
                null);

        MedicalService savedService = medicalServiceRepository.save(service);

        return toResponse(savedService);
    }

    @Transactional
    public MedicalServiceResponse updateService(
            Long serviceId,
            String email,
            UpdateMedicalServiceRequest request) {

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"));

        MedicalService service = medicalServiceRepository
                .findByIdAndUserId(serviceId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Medical service not found"));

        String name = request.name().trim();

        if (!service.getName().equalsIgnoreCase(name)
                && medicalServiceRepository
                        .existsByUserIdAndNameIgnoreCaseAndCategory(
                                user.getId(),
                                name,
                                service.getCategory())) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Medical service already exists");
        }

        service.rename(name);

        return toResponse(service);
    }

    @Transactional
    public void archiveService(Long serviceId, String email) {

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"));

        MedicalService service = medicalServiceRepository
                .findByIdAndUserId(serviceId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Medical service not found"));

        service.archive();
    }

    @Transactional
    public void restoreService(Long serviceId, String email) {

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"));

        MedicalService service = medicalServiceRepository
                .findByIdAndUserId(serviceId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Medical service not found"));

        service.restore();
    }

    @Transactional(readOnly = true)
    public List<MedicalServiceResponse> getArchivedServices(String email) {

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"));

        return medicalServiceRepository
                .findByUserIdAndActiveFalseOrderByNameAsc(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }
}
