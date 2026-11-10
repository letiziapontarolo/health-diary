package com.healthdiary.backend.auth.service;

import com.healthdiary.backend.auth.dto.RegisterRequest;
import com.healthdiary.backend.auth.dto.RegisterResponse;
import com.healthdiary.backend.auth.entity.User;
import com.healthdiary.backend.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.healthdiary.backend.auth.dto.LoginRequest;
import com.healthdiary.backend.auth.dto.LoginResponse;

import java.util.Optional;

class AuthServiceTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = new BCryptPasswordEncoder();
        authService = new AuthService(userRepository, passwordEncoder);
    }

    private RegisterRequest validRequest() {
        return new RegisterRequest(
                "Mario",
                "Rossi",
                "mario@example.com",
                "SecurePassword123!",
                LocalDate.of(1995, 5, 10));
    }

    @Test
    void shouldRegisterUserSuccessfully() {
        when(userRepository.saveAndFlush(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RegisterResponse response = authService.register(validRequest());

        assertEquals("Mario", response.firstName());
        assertEquals("Rossi", response.lastName());
        assertEquals("mario@example.com", response.email());

        verify(userRepository).saveAndFlush(any(User.class));
    }

    @Test
    void shouldRejectDuplicateEmail() {
        when(userRepository.existsByEmailIgnoreCase("mario@example.com"))
                .thenReturn(true);

        assertThrows(
                EmailAlreadyExistsException.class,
                () -> authService.register(validRequest()));

        verify(userRepository, never()).saveAndFlush(any(User.class));
    }

    @Test
    void shouldHashPasswordBeforeSaving() {
        when(userRepository.saveAndFlush(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        authService.register(validRequest());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);

        verify(userRepository).saveAndFlush(captor.capture());

        User savedUser = captor.getValue();

        assertNotEquals(
                "SecurePassword123!",
                savedUser.getPasswordHash());

        assertTrue(
                passwordEncoder.matches(
                        "SecurePassword123!",
                        savedUser.getPasswordHash()));
    }

    @Test
    void shouldNormalizeEmailBeforeSaving() {
        RegisterRequest request = new RegisterRequest(
                "Mario",
                "Rossi",
                "  MARIO@EXAMPLE.COM  ",
                "SecurePassword123!",
                LocalDate.of(1995, 5, 10));

        when(userRepository.saveAndFlush(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RegisterResponse response = authService.register(request);

        assertEquals("mario@example.com", response.email());

        verify(userRepository)
                .existsByEmailIgnoreCase("mario@example.com");
    }

    @Test
    void shouldLoginSuccessfully() {
        LoginRequest request = new LoginRequest(
                "mario@example.com",
                "PasswordTest123!");

        User user = new User(
                "Mario",
                "Rossi",
                "mario@example.com",
                passwordEncoder.encode("PasswordTest123!"),
                LocalDate.of(1995, 5, 10));

        when(userRepository.findByEmailIgnoreCase("mario@example.com"))
                .thenReturn(Optional.of(user));

        LoginResponse response = authService.login(request);

        assertEquals("mario@example.com", response.email());
        assertEquals("Mario", response.firstName());
    }

    @Test
    void shouldRejectInvalidPassword() {
        LoginRequest request = new LoginRequest(
                "mario@example.com",
                "WrongPassword123!");

        User user = new User(
                "Mario",
                "Rossi",
                "mario@example.com",
                passwordEncoder.encode("PasswordTest123!"),
                LocalDate.of(1995, 5, 10));

        when(userRepository.findByEmailIgnoreCase("mario@example.com"))
                .thenReturn(Optional.of(user));

        assertThrows(
                InvalidCredentialsException.class,
                () -> authService.login(request));
    }

    @Test
    void shouldRejectUnknownEmail() {
        LoginRequest request = new LoginRequest(
                "unknown@example.com",
                "PasswordTest123!");

        when(userRepository.findByEmailIgnoreCase("unknown@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(
                InvalidCredentialsException.class,
                () -> authService.login(request));
    }

}
