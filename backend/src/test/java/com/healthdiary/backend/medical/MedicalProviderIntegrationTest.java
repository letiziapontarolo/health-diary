package com.healthdiary.backend.medical;

import com.healthdiary.backend.auth.entity.User;
import com.healthdiary.backend.auth.repository.UserRepository;
import com.healthdiary.backend.medical.repository.DoctorRepository;
import com.healthdiary.backend.medical.repository.FacilityRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Transactional
class MedicalProviderIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:17");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private FacilityRepository facilityRepository;

    private User mario;
    private User giulia;

    @BeforeEach
    void setUp() {
        mario = userRepository.save(new User(
                "Mario", "Rossi", "mario.provider@test.com",
                "test-password-hash", LocalDate.of(1995, 5, 10)
        ));

        giulia = userRepository.save(new User(
                "Giulia", "Bianchi", "giulia.provider@test.com",
                "test-password-hash", LocalDate.of(1997, 8, 15)
        ));
    }

    @Test
    void shouldCreateDoctor() throws Exception {
        mockMvc.perform(post("/api/doctors")
                        .with(user(mario.getEmail()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "Dott. Mario Bianchi",
                                    "specialization": "Endocrinologia"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name")
                        .value("Dott. Mario Bianchi"))
                .andExpect(jsonPath("$.specialization")
                        .value("Endocrinologia"));

        assertEquals(1,
                doctorRepository.findByUserIdOrderByNameAsc(
                        mario.getId()).size());
    }

    @Test
    void shouldRejectDuplicateDoctor() throws Exception {
        mockMvc.perform(post("/api/doctors")
                        .with(user(mario.getEmail()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Dott. Rossi"}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/doctors")
                        .with(user(mario.getEmail()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "dott. rossi"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldRejectBlankDoctorName() throws Exception {
        mockMvc.perform(post("/api/doctors")
                        .with(user(mario.getEmail()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "   "}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldListOnlyOwnDoctors() throws Exception {
        doctorRepository.save(
                new com.healthdiary.backend.medical.entity.Doctor(
                        mario, "Dott. Rossi", "Cardiologia"));

        doctorRepository.save(
                new com.healthdiary.backend.medical.entity.Doctor(
                        giulia, "Dott. Verdi", "Neurologia"));

        mockMvc.perform(get("/api/doctors")
                        .with(user(mario.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name")
                        .value("Dott. Rossi"));
    }

    @Test
    void shouldCreateFacility() throws Exception {
        mockMvc.perform(post("/api/facilities")
                        .with(user(mario.getEmail()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "Ospedale Centrale",
                                    "city": "Torino"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name")
                        .value("Ospedale Centrale"))
                .andExpect(jsonPath("$.city").value("Torino"));

        assertEquals(1,
                facilityRepository.findByUserIdOrderByNameAsc(
                        mario.getId()).size());
    }

    @Test
    void shouldRejectDuplicateFacility() throws Exception {
        mockMvc.perform(post("/api/facilities")
                        .with(user(mario.getEmail()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Ospedale Centrale"}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/facilities")
                        .with(user(mario.getEmail()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "ospedale centrale"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldListOnlyOwnFacilities() throws Exception {
        facilityRepository.save(
                new com.healthdiary.backend.medical.entity.Facility(
                        mario, "Ospedale Centrale", "Torino"));

        facilityRepository.save(
                new com.healthdiary.backend.medical.entity.Facility(
                        giulia, "Clinica Privata", "Milano"));

        mockMvc.perform(get("/api/facilities")
                        .with(user(mario.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name")
                        .value("Ospedale Centrale"));
    }

    @Test
    void shouldRejectUnauthenticatedProviderRequests() throws Exception {
        mockMvc.perform(get("/api/doctors"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/facilities"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/doctors")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Dott. Rossi"}
                                """))
                .andExpect(status().isUnauthorized());
    }
}
