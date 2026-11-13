package com.healthdiary.backend.medical.service;

import com.healthdiary.backend.auth.entity.User;
import com.healthdiary.backend.auth.repository.UserRepository;
import com.healthdiary.backend.medical.dto.CreateMedicalEventRequest;
import com.healthdiary.backend.medical.dto.MedicalEventResponse;
import com.healthdiary.backend.medical.entity.*;
import com.healthdiary.backend.medical.repository.*;
import com.healthdiary.backend.medical.dto.UpdateMedicalEventRequest;
import com.healthdiary.backend.medical.dto.UpdateMedicalEventReportRequest;
import com.healthdiary.backend.medical.dto.UpdateReportNotExpectedRequest;
import com.healthdiary.backend.medical.dto.MedicalEventFilter;
import com.healthdiary.backend.document.repository.MedicalEventDocumentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.data.domain.Sort;

import java.util.List;

@Service
@Transactional
public class MedicalEventService {

        private final MedicalEventRepository eventRepository;
        private final MedicalServiceRepository serviceRepository;
        private final MedicalServiceOptionRepository optionRepository;
        private final MedicalEventOptionRepository eventOptionRepository;
        private final DoctorRepository doctorRepository;
        private final FacilityRepository facilityRepository;
        private final UserRepository userRepository;
        private final MedicalEventStatusCalculator statusCalculator;
        private final MedicalEventDocumentRepository documentLinkRepository;

        public MedicalEventService(
                        MedicalEventRepository eventRepository,
                        MedicalServiceRepository serviceRepository,
                        MedicalEventOptionRepository eventOptionRepository,
                        MedicalServiceOptionRepository optionRepository,
                        DoctorRepository doctorRepository,
                        FacilityRepository facilityRepository,
                        UserRepository userRepository,
                        MedicalEventStatusCalculator statusCalculator,
                        MedicalEventDocumentRepository documentLinkRepository) {
                this.eventRepository = eventRepository;
                this.serviceRepository = serviceRepository;
                this.optionRepository = optionRepository;
                this.eventOptionRepository = eventOptionRepository;
                this.doctorRepository = doctorRepository;
                this.facilityRepository = facilityRepository;
                this.userRepository = userRepository;
                this.statusCalculator = statusCalculator;
                this.documentLinkRepository = documentLinkRepository;
        }

        private User getUser(String email) {
                return userRepository.findByEmailIgnoreCase(email)
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.NOT_FOUND, "User not found"));
        }

        private MedicalService getAccessibleService(Long serviceId, Long userId) {
                MedicalService service = serviceRepository.findById(serviceId)
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.NOT_FOUND, "Medical service not found"));

                if (service.getUser() != null
                                && !service.getUser().getId().equals(userId)) {
                        throw new ResponseStatusException(
                                        HttpStatus.NOT_FOUND, "Medical service not found");
                }

                if (!service.isActive()) {
                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST, "Medical service is archived");
                }

                return service;
        }

        private Doctor getDoctor(Long doctorId, Long userId) {
                if (doctorId == null) {
                        return null;
                }

                return doctorRepository.findByIdAndUserId(doctorId, userId)
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.NOT_FOUND, "Doctor not found"));
        }

        private Facility getFacility(Long facilityId, Long userId) {
                if (facilityId == null) {
                        return null;
                }

                return facilityRepository.findByIdAndUserId(facilityId, userId)
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.NOT_FOUND, "Facility not found"));
        }

        private List<MedicalServiceOption> validateOptions(
                        MedicalService service,
                        List<Long> optionIds) {
                if (optionIds == null || optionIds.isEmpty()) {
                        return List.of();
                }

                if (service.getCategory() != MedicalServiceCategory.DIAGNOSTIC) {
                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "Options are allowed only for diagnostic services");
                }

                if (optionIds.stream().anyMatch(id -> id == null)) {
                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "Option IDs cannot be null");
                }

                if (optionIds.stream().distinct().count() != optionIds.size()) {
                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "Duplicate options are not allowed");
                }

                if (service.getOptionSelectionMode() == OptionSelectionMode.SINGLE
                                && optionIds.size() > 1) {
                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "Only one option can be selected");
                }

                List<MedicalServiceOption> options = optionRepository.findAllById(optionIds);

                if (options.size() != optionIds.size()) {
                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "One or more options were not found");
                }

                for (MedicalServiceOption option : options) {
                        if (!option.getService().getId().equals(service.getId())) {
                                throw new ResponseStatusException(
                                                HttpStatus.BAD_REQUEST,
                                                "Option does not belong to the selected service");
                        }

                        if (!option.isActive()) {
                                throw new ResponseStatusException(
                                                HttpStatus.BAD_REQUEST,
                                                "Archived options cannot be selected");
                        }
                }

                return options;
        }

        private void saveOptions(
                        MedicalEvent event,
                        List<MedicalServiceOption> options) {
                for (MedicalServiceOption option : options) {
                        eventOptionRepository.save(
                                        new MedicalEventOption(event, option));
                }
        }

        private void synchronizeAutomaticStatuses(Long userId) {
                List<MedicalEvent> events = eventRepository
                                .findByUserIdAndStatusAndStatusMode(
                                                userId,
                                                MedicalEventStatus.SCHEDULED,
                                                MedicalEventStatusMode.AUTO);

                for (MedicalEvent event : events) {
                        MedicalEventStatus calculatedStatus = statusCalculator.calculate(
                                        event.getEventDate(),
                                        event.getEventTime());

                        if (calculatedStatus == MedicalEventStatus.COMPLETED) {
                                event.updateAutomaticStatus(calculatedStatus);
                        }
                }
        }

        public MedicalEventResponse createEvent(
                        String email,
                        CreateMedicalEventRequest request) {
                User user = getUser(email);

                MedicalService service = getAccessibleService(
                                request.serviceId(),
                                user.getId());

                Doctor doctor = getDoctor(request.doctorId(), user.getId());
                Facility facility = getFacility(request.facilityId(), user.getId());

                List<MedicalServiceOption> options = validateOptions(service, request.optionIds());

                MedicalEvent event = new MedicalEvent(
                                user,
                                service,
                                request.eventDate(),
                                request.eventTime(),
                                doctor,
                                facility,
                                request.notes());

                event.updateAutomaticStatus(
                                statusCalculator.calculate(
                                                event.getEventDate(),
                                                event.getEventTime()));

                event = eventRepository.save(event);

                saveOptions(event, options);

                return toResponse(event);
        }

        public MedicalEventResponse updateEvent(
                        Long eventId,
                        String email,
                        UpdateMedicalEventRequest request) {
                User user = getUser(email);

                MedicalEvent event = eventRepository
                                .findByIdAndUserId(eventId, user.getId())
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "Medical event not found"));

                MedicalService service = getAccessibleService(
                                request.serviceId(),
                                user.getId());

                Doctor doctor = getDoctor(
                                request.doctorId(),
                                user.getId());

                Facility facility = getFacility(
                                request.facilityId(),
                                user.getId());

                List<MedicalServiceOption> options = validateOptions(service, request.optionIds());

                // 1. Remove existing diagnostic options.
                eventOptionRepository.deleteByMedicalEventId(eventId);
                eventOptionRepository.flush();

                // 2. Update the medical event.
                event.update(
                                service,
                                request.eventDate(),
                                request.eventTime(),
                                doctor,
                                facility,
                                request.notes());

                event.updateAutomaticStatus(
                                statusCalculator.calculate(
                                                event.getEventDate(),
                                                event.getEventTime()));

                // 3. Persist the updated service before inserting new options.
                eventRepository.flush();

                // 4. Save the options belonging to the new service.
                saveOptions(event, options);

                return toResponse(event);
        }

        public List<MedicalEventResponse> getEvents(String email) {
                User user = getUser(email);

                List<MedicalEvent> events = eventRepository
                                .findByUserIdOrderByEventDateDescEventTimeDesc(user.getId());

                for (MedicalEvent event : events) {
                        MedicalEventStatus calculatedStatus = statusCalculator.calculate(
                                        event.getEventDate(),
                                        event.getEventTime());

                        event.updateAutomaticStatus(calculatedStatus);
                }

                return events.stream()
                                .map(this::toResponse)
                                .toList();
        }

        public List<MedicalEventResponse> getFilteredEvents(
                        String email,
                        MedicalEventFilter filter) {
                User user = getUser(email);
                synchronizeAutomaticStatuses(user.getId());

                if (filter.fromDate() != null
                                && filter.toDate() != null
                                && filter.fromDate().isAfter(filter.toDate())) {
                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "fromDate cannot be after toDate");
                }
                eventRepository.flush();
                return eventRepository.findAll(
                                MedicalEventSpecifications.withFilters(
                                                user.getId(),
                                                filter),
                                Sort.by(
                                                Sort.Order.desc("eventDate"),
                                                Sort.Order.desc("eventTime"),
                                                Sort.Order.desc("id")))
                                .stream()
                                .map(this::toResponse)
                                .toList();
        }

        public MedicalEventResponse getEvent(Long eventId, String email) {
                User user = getUser(email);

                MedicalEvent event = eventRepository
                                .findByIdAndUserId(eventId, user.getId())
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.NOT_FOUND, "Medical event not found"));

                MedicalEventStatus calculatedStatus = statusCalculator.calculate(
                                event.getEventDate(),
                                event.getEventTime());

                event.updateAutomaticStatus(calculatedStatus);

                return toResponse(event);
        }

        private MedicalEventResponse toResponse(MedicalEvent event) {

                boolean reportMissing = event.isReportMissing();

                if (reportMissing) {
                        boolean hasLinkedReport = documentLinkRepository.existsReportForMedicalEvent(
                                        event.getId(),
                                        event.getUser().getId());

                        reportMissing = !hasLinkedReport;
                }

                return new MedicalEventResponse(
                                event.getId(),
                                event.getService().getId(),
                                event.getService().getName(),
                                event.getService().getCategory().name(),
                                event.getEventDate(),
                                event.getEventTime(),
                                event.getStatus(),
                                event.getStatusMode(),
                                event.getDoctor() != null ? event.getDoctor().getId() : null,
                                event.getDoctor() != null ? event.getDoctor().getName() : null,
                                event.getFacility() != null ? event.getFacility().getId() : null,
                                event.getFacility() != null ? event.getFacility().getName() : null,
                                event.getNotes(),
                                event.getReportText(),
                                event.isReportNotExpected(),
                                reportMissing,
                                eventOptionRepository
                                                .findByMedicalEventIdOrderByOptionNameAsc(event.getId())
                                                .stream()
                                                .map(link -> link.getOption().getId())
                                                .toList());
        }

        public MedicalEventResponse cancelEvent(Long eventId, String email) {
                User user = getUser(email);

                MedicalEvent event = eventRepository
                                .findByIdAndUserId(eventId, user.getId())
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "Medical event not found"));

                if (event.getStatus() == MedicalEventStatus.CANCELLED) {
                        throw new ResponseStatusException(
                                        HttpStatus.CONFLICT,
                                        "Medical event is already cancelled");
                }

                event.cancel();

                return toResponse(event);
        }

        public MedicalEventResponse restoreEvent(Long eventId, String email) {
                User user = getUser(email);

                MedicalEvent event = eventRepository
                                .findByIdAndUserId(eventId, user.getId())
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "Medical event not found"));

                if (event.getStatus() != MedicalEventStatus.CANCELLED) {
                        throw new ResponseStatusException(
                                        HttpStatus.CONFLICT,
                                        "Medical event is not cancelled");
                }

                MedicalEventStatus restoredStatus = statusCalculator.calculate(
                                event.getEventDate(),
                                event.getEventTime());

                event.restore(restoredStatus);

                return toResponse(event);
        }

        public MedicalEventResponse updateReport(
                        Long eventId,
                        String email,
                        UpdateMedicalEventReportRequest request) {
                User user = getUser(email);

                MedicalEvent event = eventRepository
                                .findByIdAndUserId(eventId, user.getId())
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "Medical event not found"));

                event.updateReportText(request.reportText());

                return toResponse(event);
        }

        public MedicalEventResponse deleteReport(
                        Long eventId,
                        String email) {
                User user = getUser(email);

                MedicalEvent event = eventRepository
                                .findByIdAndUserId(eventId, user.getId())
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "Medical event not found"));

                event.deleteReportText();

                return toResponse(event);
        }

        public MedicalEventResponse updateReportNotExpected(
                        Long eventId,
                        String email,
                        UpdateReportNotExpectedRequest request) {
                User user = getUser(email);

                MedicalEvent event = eventRepository
                                .findByIdAndUserId(eventId, user.getId())
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "Medical event not found"));

                event.setReportNotExpected(request.reportNotExpected());

                return toResponse(event);
        }

        public void deleteEvent(Long eventId, String email) {
                User user = getUser(email);

                MedicalEvent event = eventRepository
                                .findByIdAndUserId(eventId, user.getId())
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "Medical event not found"));

                eventOptionRepository.deleteByMedicalEventId(eventId);
                eventOptionRepository.flush();

                eventRepository.delete(event);
        }

}
