package com.healthdiary.backend.medical;

import com.healthdiary.backend.auth.entity.User;
import com.healthdiary.backend.auth.repository.UserRepository;
import com.healthdiary.backend.medical.entity.Doctor;
import com.healthdiary.backend.medical.repository.DoctorRepository;
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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Transactional
class GeneralPractitionerIntegrationTest {
        @Container
        @ServiceConnection
        static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");
        @Autowired
        MockMvc mvc;
        @Autowired
        UserRepository users;
        @Autowired
        DoctorRepository doctors;
        User mario, giulia;
        Doctor oldDoctor, newDoctor, otherUserDoctor;

        @BeforeEach
        void setup() {
                mario = users.save(new User("Mario", "Rossi", "mario.gp@test.com", "hash", LocalDate.of(1995, 1, 1)));
                giulia = users.save(
                                new User("Giulia", "Bianchi", "giulia.gp@test.com", "hash", LocalDate.of(1996, 1, 1)));
                oldDoctor = doctors.save(new Doctor(mario, "Dott.ssa Vecchia", null));
                newDoctor = doctors.save(new Doctor(mario, "Dott.ssa Nuova", null));
                otherUserDoctor = doctors.save(new Doctor(giulia, "Dott. Altro", null));
        }

        @Test
        void startsEmpty() throws Exception {
                mvc.perform(get("/api/general-practitioner/history").with(user(mario.getEmail())))
                                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
                mvc.perform(get("/api/general-practitioner/current").with(user(mario.getEmail())))
                                .andExpect(status().isOk()).andExpect(content().string(""));
        }

        @Test
        void changesAndPreservesHistory() throws Exception {
                mvc.perform(post("/api/general-practitioner").with(user(mario.getEmail())).with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"doctorId\":" + oldDoctor.getId() + ",\"startDate\":\"2020-01-01\"}"))
                                .andExpect(status().isCreated()).andExpect(jsonPath("$.current").value(true));
                mvc.perform(post("/api/general-practitioner").with(user(mario.getEmail())).with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"doctorId\":" + newDoctor.getId() + ",\"startDate\":\"2026-09-01\"}"))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.doctorId").value(newDoctor.getId()));
                mvc.perform(get("/api/general-practitioner/history").with(user(mario.getEmail())))
                                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2))
                                .andExpect(jsonPath("$[0].current").value(true))
                                .andExpect(jsonPath("$[1].doctorId").value(oldDoctor.getId()))
                                .andExpect(jsonPath("$[1].endDate").value("2026-08-31"));
        }

        @Test
        void rejectsOtherUsersDoctorAndDuplicateCurrent() throws Exception {
                mvc.perform(post("/api/general-practitioner").with(user(mario.getEmail())).with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"doctorId\":" + otherUserDoctor.getId() + "}"))
                                .andExpect(status().isNotFound());
                mvc.perform(post("/api/general-practitioner").with(user(mario.getEmail())).with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"doctorId\":" + oldDoctor.getId() + "}"))
                                .andExpect(status().isCreated());
                mvc.perform(post("/api/general-practitioner").with(user(mario.getEmail())).with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"doctorId\":" + oldDoctor.getId() + "}"))
                                .andExpect(status().isConflict());
                mvc.perform(get("/api/general-practitioner/history").with(user(giulia.getEmail())))
                                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        void requiresAuthenticationAndCsrf() throws Exception {
                mvc.perform(get("/api/general-practitioner/history")).andExpect(status().isUnauthorized());
                mvc.perform(post("/api/general-practitioner").with(user(mario.getEmail()))
                                .contentType(MediaType.APPLICATION_JSON).content("{\"doctorId\":1}"))
                                .andExpect(status().isForbidden());
        }
}
