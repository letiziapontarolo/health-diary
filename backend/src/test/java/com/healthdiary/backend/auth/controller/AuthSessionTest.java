package com.healthdiary.backend.auth.controller;

import com.healthdiary.backend.auth.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.healthdiary.backend.auth.entity.User;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class AuthSessionTest {

    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17")
            .withDatabaseName("health_diary_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void shouldRejectUnauthenticatedRequest() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldKeepAuthenticationAcrossRequests() throws Exception {

        User user = new User(
                "Session",
                "Test",
                "session.test@example.com",
                passwordEncoder.encode("PasswordTest123!"),
                LocalDate.of(1995, 5, 10));

        userRepository.saveAndFlush(user);

        try {
            String loginJson = """
                    {
                        "email": "session.test@example.com",
                        "password": "PasswordTest123!"
                    }
                    """;

            var loginResult = mockMvc.perform(
                    post("/api/auth/login")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email")
                            .value("session.test@example.com"))
                    .andReturn();

            HttpSession session = loginResult.getRequest().getSession(false);

            assertNotNull(session);

            mockMvc.perform(
                    get("/api/auth/me")
                            .session((MockHttpSession) session))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email")
                            .value("session.test@example.com"));

        } finally {
            userRepository.deleteById(user.getId());
        }
    }

    @Test
    void shouldRejectLoginWithoutCsrfToken() throws Exception {
        String loginJson = """
                {
                    "email": "session.test@example.com",
                    "password": "PasswordTest123!"
                }
                """;

        mockMvc.perform(
                post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectLoginWithInvalidCredentials() throws Exception {
        String loginJson = """
                {
                    "email": "nonexistent@example.com",
                    "password": "WrongPassword123!"
                }
                """;

        mockMvc.perform(
                post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error")
                        .value("INVALID_CREDENTIALS"));
    }

    @Test
    void shouldRejectInvalidLoginRequest() throws Exception {
        String loginJson = """
                {
                    "email": "not-an-email",
                    "password": ""
                }
                """;

        mockMvc.perform(
                post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectLogoutWithoutCsrfToken() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldInvalidateSessionAfterLogout() throws Exception {

        User user = new User(
                "Logout",
                "Test",
                "logout.test@example.com",
                passwordEncoder.encode("PasswordTest123!"),
                LocalDate.of(1995, 5, 10));

        userRepository.saveAndFlush(user);

        try {
            String loginJson = """
                    {
                        "email": "logout.test@example.com",
                        "password": "PasswordTest123!"
                    }
                    """;

            // 1. Effettuiamo il login
            var loginResult = mockMvc.perform(
                    post("/api/auth/login")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginJson))
                    .andExpect(status().isOk())
                    .andReturn();

            MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);

            assertNotNull(session);

            // 2. Verifichiamo che la sessione sia autenticata
            mockMvc.perform(
                    get("/api/auth/me")
                            .session(session))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email")
                            .value("logout.test@example.com"));

            // 3. Effettuiamo il logout
            mockMvc.perform(
                    post("/api/auth/logout")
                            .session(session)
                            .with(csrf()))
                    .andExpect(status().isNoContent());

            // 4. Verifichiamo che la sessione sia stata invalidata
            assertTrue(session.isInvalid());

        } finally {
            userRepository.deleteById(user.getId());
        }
    }
}
