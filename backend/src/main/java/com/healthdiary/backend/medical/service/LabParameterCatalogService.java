package com.healthdiary.backend.medical.service;

import com.healthdiary.backend.auth.entity.User;
import com.healthdiary.backend.auth.repository.UserRepository;
import com.healthdiary.backend.medical.dto.LabParameterResponse;
import com.healthdiary.backend.medical.entity.LabParameter;
import com.healthdiary.backend.medical.entity.MedicalService;
import com.healthdiary.backend.medical.entity.MedicalServiceCategory;
import com.healthdiary.backend.medical.repository.LabParameterRepository;
import com.healthdiary.backend.medical.repository.MedicalServiceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.healthdiary.backend.medical.dto.CreateLabParameterRequest;
import com.healthdiary.backend.medical.dto.UpdateLabParameterRequest;

import java.util.List;

@Service
public class LabParameterCatalogService {

    private final LabParameterRepository labParameterRepository;
    private final MedicalServiceRepository medicalServiceRepository;
    private final UserRepository userRepository;

    public LabParameterCatalogService(
            LabParameterRepository labParameterRepository,
            MedicalServiceRepository medicalServiceRepository,
            UserRepository userRepository) {

        this.labParameterRepository = labParameterRepository;
        this.medicalServiceRepository = medicalServiceRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<LabParameterResponse> getDefaultParameters(
            Long serviceId) {

        MedicalService service = medicalServiceRepository
                .findByIdAndUserIsNullAndActiveTrue(serviceId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Medical service not found"));

        validateLaboratoryService(service);

        return labParameterRepository
                .findByServiceIdAndUserIsNullAndActiveTrueOrderByNameAsc(
                        serviceId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LabParameterResponse> getUserParameters(
            Long serviceId,
            String email) {

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"));

        MedicalService service = medicalServiceRepository
                .findById(serviceId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Medical service not found"));

        if (!service.isActive()
                || (service.getUser() != null
                        && !service.getUser().getId().equals(user.getId()))) {

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Medical service not found");
        }

        validateLaboratoryService(service);

        return labParameterRepository
                .findByServiceIdAndUserIdAndActiveTrueOrderByNameAsc(
                        serviceId,
                        user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private void validateLaboratoryService(MedicalService service) {
        if (service.getCategory() != MedicalServiceCategory.LABORATORY) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Medical service is not a laboratory service");
        }
    }

    private LabParameterResponse toResponse(LabParameter parameter) {
        return new LabParameterResponse(
                parameter.getId(),
                parameter.getService().getId(),
                parameter.getName(),
                parameter.getGroupKey(),
                parameter.isActive(),
                parameter.getUser() != null);
    }

    @Transactional
    public LabParameterResponse createParameter(
            String email,
            CreateLabParameterRequest request) {

        User user = findUser(email);
        MedicalService service = findAccessibleLaboratoryService(
                request.serviceId(), user);

        String name = request.name().trim();

        validateUniqueName(service.getId(), user.getId(), name);

        String groupKey = request.groupKey() == null
                ? null
                : request.groupKey().trim();

        if (groupKey != null && groupKey.isEmpty()) {
            groupKey = null;
        }

        // Custom parameters do not need a predefined group.
        // Group validation will be added before accepting non-null values.
        if (groupKey != null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Custom group keys are not supported yet");
        }

        LabParameter parameter = new LabParameter(
                service, user, name, groupKey);

        return toResponse(labParameterRepository.save(parameter));
    }

    @Transactional
    public LabParameterResponse updateParameter(
            Long parameterId,
            String email,
            UpdateLabParameterRequest request) {

        User user = findUser(email);
        LabParameter parameter = findOwnedParameter(parameterId, user.getId());

        String name = request.name().trim();

        if (!parameter.getName().equalsIgnoreCase(name)) {
            validateUniqueName(
                    parameter.getService().getId(),
                    user.getId(),
                    name);
        }

        parameter.rename(name);

        return toResponse(parameter);
    }

    @Transactional
    public void archiveParameter(Long parameterId, String email) {
        User user = findUser(email);
        LabParameter parameter = findOwnedParameter(parameterId, user.getId());
        parameter.archive();
    }

    @Transactional
    public void restoreParameter(Long parameterId, String email) {
        User user = findUser(email);
        LabParameter parameter = findOwnedParameter(parameterId, user.getId());
        parameter.restore();
    }

    private User findUser(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"));
    }

    private LabParameter findOwnedParameter(Long parameterId, Long userId) {
        return labParameterRepository.findByIdAndUserId(parameterId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Lab parameter not found"));
    }

    private MedicalService findAccessibleLaboratoryService(
            Long serviceId, User user) {

        MedicalService service = medicalServiceRepository.findById(serviceId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Medical service not found"));

        if (!service.isActive()
                || (service.getUser() != null
                        && !service.getUser().getId().equals(user.getId()))) {

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Medical service not found");
        }

        validateLaboratoryService(service);
        return service;
    }

    private void validateUniqueName(
            Long serviceId, Long userId, String name) {

        if (labParameterRepository
                .existsByServiceIdAndUserIdAndNameIgnoreCase(
                        serviceId, userId, name)) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Lab parameter already exists");
        }
    }

    @Transactional(readOnly = true)
    public List<LabParameterResponse> getArchivedParameters(String email) {
        User user = findUser(email);

        return labParameterRepository
                .findByUserIdAndActiveFalseOrderByNameAsc(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

}
