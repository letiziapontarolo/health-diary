package com.healthdiary.backend.medical.service;

import com.healthdiary.backend.auth.entity.User;
import com.healthdiary.backend.auth.repository.UserRepository;
import com.healthdiary.backend.medical.dto.*;
import com.healthdiary.backend.medical.entity.*;
import com.healthdiary.backend.medical.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LabMicrobiologyServiceTest {
    @Mock
    MicrobiologyAccess access;
    @Mock
    LabResultOrganismRepository organisms;
    @Mock
    LabMicrobiologyTargetRepository targets;
    @InjectMocks
    LabMicrobiologyService service;

    @Test
    void rejectsInvalidOrganismType() {
        var request = new OrganismRequest("VIRUS", "Example", 1, null, null, null, null, null, null, null);
        assertEquals(HttpStatus.BAD_REQUEST,
                assertThrows(ResponseStatusException.class, () -> service.addOrganism(1L, "u@test.it", request))
                        .getStatusCode());
        verify(organisms, never()).save(any());
    }

    @Test
    void rejectsInvalidTargetResult() {
        var request = new MicrobiologyTargetRequest(1, "Influenza", "UNKNOWN", null, null, null);
        assertEquals(HttpStatus.BAD_REQUEST,
                assertThrows(ResponseStatusException.class, () -> service.addTarget(1L, "u@test.it", request))
                        .getStatusCode());
        verify(targets, never()).save(any());
    }

    @Test
    void rejectsDuplicateOrganismPosition() {
        var request = new OrganismRequest("BACTERIUM", "Example", 1, null, null, null, null, null, null, null);
        when(organisms.existsByExamResultIdAndPosition(1L, 1)).thenReturn(true);
        assertEquals(HttpStatus.CONFLICT,
                assertThrows(ResponseStatusException.class, () -> service.addOrganism(1L, "u@test.it", request))
                        .getStatusCode());
    }

    @Test
    void rejectsDuplicateTargetPosition() {
        var request = new MicrobiologyTargetRequest(1, "Influenza", "NEGATIVE", null, null, null);
        when(targets.existsByExamResultIdAndPosition(1L, 1)).thenReturn(true);
        assertEquals(HttpStatus.CONFLICT,
                assertThrows(ResponseStatusException.class, () -> service.addTarget(1L, "u@test.it", request))
                        .getStatusCode());
    }

    @Test
    void listsEmptyOrganisms() {
        when(organisms.findByExamResultIdOrderByPositionAsc(1L)).thenReturn(java.util.List.of());
        assertTrue(service.organisms(1L, "u@test.it").isEmpty());
        verify(access).ownedResult(1L, "u@test.it");
    }

    @Test
    void listsEmptyTargets() {
        when(targets.findByExamResultIdOrderByPositionAsc(1L)).thenReturn(java.util.List.of());
        assertTrue(service.targets(1L, "u@test.it").isEmpty());
        verify(access).ownedResult(1L, "u@test.it");
    }

    @Test
    void rejectsNegativeOrganismQuantity() {
        var r = new OrganismRequest("BACTERIUM", "E. coli", 1, null, new java.math.BigDecimal("-1"), null, null, null,
                null, null);
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
                () -> service.addOrganism(1L, "u@test.it", r)).getStatusCode());
    }

    @Test
    void rejectsBlankTargetName() {
        var r = new MicrobiologyTargetRequest(1, "  ", "NEGATIVE", null, null, null);
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
                () -> service.addTarget(1L, "u@test.it", r)).getStatusCode());
    }

    @Test
    void addOrganismPersistsTrimmedName() {
        ExamResult result = mock(ExamResult.class);
        when(result.getId()).thenReturn(1L);
        when(access.ownedResult(1L, "u@test.it")).thenReturn(result);
        when(organisms.save(any(LabResultOrganism.class))).thenAnswer(i -> i.getArgument(0));
        var r = new OrganismRequest("BACTERIUM", "  E. coli  ", 1, null, null, null, null, null, null, null);
        var response = service.addOrganism(1L, "u@test.it", r);
        assertEquals("E. coli", response.organismName());
    }

    @Test
    void addTargetPersistsTrimmedName() {
        ExamResult result = mock(ExamResult.class);
        when(result.getId()).thenReturn(1L);
        when(access.ownedResult(1L, "u@test.it")).thenReturn(result);
        when(targets.save(any(LabMicrobiologyTarget.class))).thenAnswer(i -> i.getArgument(0));
        var r = new MicrobiologyTargetRequest(1, "  Influenza A  ", "NEGATIVE", null, null, null);
        assertEquals("Influenza A", service.addTarget(1L, "u@test.it", r).targetName());
    }

    @Test
    void editOrganismUpdatesExistingEntity() {
        ExamResult result = mock(ExamResult.class);
        when(result.getId()).thenReturn(1L);
        LabResultOrganism organism = new LabResultOrganism(result, "BACTERIUM", "Old", 1, null, null, null, null, null,
                null, null);
        when(organisms.findByIdAndExamResultId(5L, 1L)).thenReturn(Optional.of(organism));
        var r = new OrganismRequest("FUNGUS", "Candida", 2, null, null, null, null, null, null, null);
        var response = service.editOrganism(1L, 5L, "u@test.it", r);
        assertEquals("Candida", response.organismName());
        assertEquals(2, response.position());
    }

    @Test
    void editTargetUpdatesExistingEntity() {
        ExamResult result = mock(ExamResult.class);
        when(result.getId()).thenReturn(1L);
        LabMicrobiologyTarget target = new LabMicrobiologyTarget(result, 1, "Old", "NEGATIVE", null, null, null);
        when(targets.findByIdAndExamResultId(5L, 1L)).thenReturn(Optional.of(target));
        var r = new MicrobiologyTargetRequest(2, "New", "POSITIVE", null, null, null);
        var response = service.editTarget(1L, 5L, "u@test.it", r);
        assertEquals("New", response.targetName());
        assertEquals("POSITIVE", response.result());
    }

    @Test
    void editOrganismRejectsOccupiedPosition() {
        ExamResult result = mock(ExamResult.class);
        when(organisms.findByIdAndExamResultId(5L, 1L)).thenReturn(Optional.of(
                new LabResultOrganism(result, "BACTERIUM", "Old", 1, null, null, null, null, null, null, null)));
        when(organisms.existsByExamResultIdAndPositionAndIdNot(1L, 2, 5L)).thenReturn(true);
        var r = new OrganismRequest("BACTERIUM", "New", 2, null, null, null, null, null, null, null);
        assertEquals(HttpStatus.CONFLICT, assertThrows(ResponseStatusException.class,
                () -> service.editOrganism(1L, 5L, "u@test.it", r)).getStatusCode());
    }

    @Test
    void deleteOrganismRemovesOwnedEntity() {
        LabResultOrganism organism = mock(LabResultOrganism.class);
        when(organisms.findByIdAndExamResultId(5L, 1L)).thenReturn(Optional.of(organism));
        service.deleteOrganism(1L, 5L, "u@test.it");
        verify(organisms).delete(organism);
    }

    @Test
    void deleteTargetRemovesOwnedEntity() {
        LabMicrobiologyTarget target = mock(LabMicrobiologyTarget.class);
        when(targets.findByIdAndExamResultId(5L, 1L)).thenReturn(Optional.of(target));
        service.deleteTarget(1L, 5L, "u@test.it");
        verify(targets).delete(target);
    }

}