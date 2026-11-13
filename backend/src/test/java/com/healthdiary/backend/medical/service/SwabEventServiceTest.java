package com.healthdiary.backend.medical.service;

import com.healthdiary.backend.auth.entity.User;
import com.healthdiary.backend.auth.repository.UserRepository;
import com.healthdiary.backend.medical.dto.SwabEventRequest;
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
class SwabEventServiceTest {
    @Mock
    SwabSiteRepository sites;
    @Mock
    MedicalEventSwabRepository swabs;
    @Mock
    MedicalEventRepository events;
    @Mock
    UserRepository users;
    @InjectMocks
    SwabEventService service;

    private void owned() {
        User user = mock(User.class);
        MedicalEvent event = mock(MedicalEvent.class);
        when(users.findByEmailIgnoreCase("u@test.it")).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(7L);
        when(events.findByIdAndUserId(10L, 7L)).thenReturn(Optional.of(event));
        when(event.getService())
                .thenReturn(new MedicalService(null, "Analisi su tampone", MedicalServiceCategory.LABORATORY, null));
    }

    @Test
    void deniesForeignEvent() {
        User user = mock(User.class);
        when(users.findByEmailIgnoreCase("u@test.it")).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(7L);
        assertEquals(HttpStatus.NOT_FOUND,
                assertThrows(ResponseStatusException.class, () -> service.get(10L, "u@test.it")).getStatusCode());
    }

    @Test
    void rejectsMissingSiteId() {
        owned();
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
                () -> service.save(10L, "u@test.it", new SwabEventRequest(null, null))).getStatusCode());
    }

    @Test
    void rejectsInactiveOrUnknownSite() {
        owned();
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
                () -> service.save(10L, "u@test.it", new SwabEventRequest(99L, null))).getStatusCode());
    }

    @Test
    void rejectsMissingSwabAssociation() {
        owned();
        assertEquals(HttpStatus.NOT_FOUND,
                assertThrows(ResponseStatusException.class, () -> service.get(10L, "u@test.it")).getStatusCode());
    }

    @Test
    void listsEmptyActiveSites() {
        when(sites.findByActiveTrueOrderByNameAsc()).thenReturn(java.util.List.of());
        assertTrue(service.sites().isEmpty());
    }

    private SwabSite site(Long id, String name, boolean active) {
        SwabSite site = mock(SwabSite.class);
        lenient().when(site.getId()).thenReturn(id);
        lenient().when(site.getName()).thenReturn(name);
        lenient().when(site.isActive()).thenReturn(active);
        return site;
    }

    @Test
    void rejectsNullRequest() {
        owned();
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
                () -> service.save(10L, "u@test.it", null)).getStatusCode());
    }

    @Test
    void rejectsOtherSiteWithoutDescription() {
        owned();
        SwabSite swabSite = site(2L, "Altro", true);
        when(sites.findById(2L)).thenReturn(Optional.of(swabSite));
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
                () -> service.save(10L, "u@test.it", new SwabEventRequest(2L, "   "))).getStatusCode());
    }

    @Test
    void rejectsDescriptionForStandardSite() {
        owned();
        SwabSite swabSite = site(2L, "Faringeo", true);
        when(sites.findById(2L)).thenReturn(Optional.of(swabSite));
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
                () -> service.save(10L, "u@test.it", new SwabEventRequest(2L, "extra"))).getStatusCode());
    }

    @Test
    void saveCreatesAssociationAndNormalizesOtherText() {
        owned();
        SwabSite site = site(2L, "Altro", true);
        when(sites.findById(2L)).thenReturn(Optional.of(site));
        when(swabs.save(any(MedicalEventSwab.class))).thenAnswer(i -> i.getArgument(0));
        var response = service.save(10L, "u@test.it", new SwabEventRequest(2L, "  Cutaneo  "));
        assertEquals(2L, response.siteId());
        assertEquals("Cutaneo", response.otherSiteText());
        verify(swabs).save(any(MedicalEventSwab.class));
    }

    @Test
    void saveUpdatesExistingAssociation() {
        owned();
        SwabSite oldSite = site(1L, "Faringeo", true), newSite = site(2L, "Nasale", true);
        MedicalEventSwab association = new MedicalEventSwab(mock(MedicalEvent.class), oldSite, null);
        when(sites.findById(2L)).thenReturn(Optional.of(newSite));
        when(swabs.findById(10L)).thenReturn(Optional.of(association));
        when(swabs.save(association)).thenReturn(association);
        var response = service.save(10L, "u@test.it", new SwabEventRequest(2L, null));
        assertEquals(2L, response.siteId());
        assertSame(newSite, association.getSite());
    }

    @Test
    void deleteRemovesOwnedAssociation() {
        owned();
        service.delete(10L, "u@test.it");
        verify(swabs).deleteById(10L);
    }

    @Test
    void rejectsNonSwabLaboratoryExam() {
        User user = mock(User.class);
        MedicalEvent event = mock(MedicalEvent.class);
        when(users.findByEmailIgnoreCase("u@test.it")).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(7L);
        when(events.findByIdAndUserId(10L, 7L)).thenReturn(Optional.of(event));
        when(event.getService())
                .thenReturn(new MedicalService(null, "Analisi del sangue", MedicalServiceCategory.LABORATORY, null));
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
                () -> service.get(10L, "u@test.it")).getStatusCode());
    }

}