package com.healthdiary.backend.auth.controller;

import com.healthdiary.backend.auth.dto.RegisterResponse;
import com.healthdiary.backend.auth.service.AuthService;
import com.healthdiary.backend.config.SecurityConfig;
import com.healthdiary.backend.auth.service.EmailAlreadyExistsException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.context.annotation.Import;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @Test
    void shouldReturnCreatedForValidRegistration() throws Exception {
        RegisterResponse response = new RegisterResponse(
                1L,
                "Mario",
                "Rossi",
                "mario@example.com");

        when(authService.register(any()))
                .thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "firstName": "Mario",
                          "lastName": "Rossi",
                          "email": "mario@example.com",
                          "password": "SecurePassword123!",
                          "dateOfBirth": "1995-05-10"
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("mario@example.com"))
                .andExpect(jsonPath("$.firstName").value("Mario"));
    }

    @Test
    void shouldReturnConflictForDuplicateEmail() throws Exception {
        when(authService.register(any()))
                .thenThrow(new EmailAlreadyExistsException());

        mockMvc.perform(post("/api/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "firstName": "Mario",
                          "lastName": "Rossi",
                          "email": "mario@example.com",
                          "password": "SecurePassword123!",
                          "dateOfBirth": "1995-05-10"
                        }
                        """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error")
                        .value("EMAIL_ALREADY_EXISTS"));
    }

    @Test
    void shouldReturnBadRequestForInvalidRegistration() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "firstName": "Mario",
                          "lastName": "Rossi",
                          "email": "mario@example.com",
                          "password": "123",
                          "dateOfBirth": "1995-05-10"
                        }
                        """))
                .andExpect(status().isBadRequest());

        verify(authService, never()).register(any());
    }

    @Test
    void shouldReturnForbiddenWithoutCsrfToken() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "firstName": "Mario",
                          "lastName": "Rossi",
                          "email": "mario@example.com",
                          "password": "SecurePassword123!",
                          "dateOfBirth": "1995-05-10"
                        }
                        """))
                .andExpect(status().isForbidden());

        verify(authService, never()).register(any());
    }

}
