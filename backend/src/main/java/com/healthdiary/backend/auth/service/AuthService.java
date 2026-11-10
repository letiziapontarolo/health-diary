
package com.healthdiary.backend.auth.service;

import com.healthdiary.backend.auth.dto.RegisterRequest;
import com.healthdiary.backend.auth.dto.RegisterResponse;
import com.healthdiary.backend.auth.entity.User;
import com.healthdiary.backend.auth.repository.UserRepository;
import com.healthdiary.backend.auth.dto.LoginRequest;
import com.healthdiary.backend.auth.dto.LoginResponse;

import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public RegisterResponse register(RegisterRequest request) {

        String email = request.email().trim()
                .toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new EmailAlreadyExistsException();
        }

        String passwordHash = passwordEncoder.encode(
                request.password());

        User user = new User(
                request.firstName().trim(),
                request.lastName().trim(),
                email,
                passwordHash,
                request.dateOfBirth());

        User savedUser = userRepository.saveAndFlush(user);

        return new RegisterResponse(
                savedUser.getId(),
                savedUser.getFirstName(),
                savedUser.getLastName(),
                savedUser.getEmail());

    }

    public LoginResponse login(LoginRequest request) {

        User user = userRepository
                .findByEmailIgnoreCase(request.email().trim())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(
                request.password(),
                user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        return new LoginResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail());
    }

    public LoginResponse getCurrentUser(String email) {
        User user = userRepository
                .findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Authenticated user not found"));

        return new LoginResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail());
    }
}
