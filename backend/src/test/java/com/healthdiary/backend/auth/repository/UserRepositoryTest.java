package com.healthdiary.backend.auth.repository;

import com.healthdiary.backend.auth.entity.User;

import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldFindUserByEmailIgnoringCase() {

        // Arrange
        User user = new User(
                "Mario",
                "Rossi",
                "mario@example.com",
                "hashed-password",
                LocalDate.of(1995, 5, 15));

        userRepository.save(user);

        // Act
        var result = userRepository.findByEmailIgnoreCase("MARIO@EXAMPLE.COM");

        // Assert
        assertThat(result).isPresent();
    }

    @Test
    void shouldDetectExistingEmailIgnoringCase() {

        // Arrange
        User user = new User(
                "Giulia",
                "Bianchi",
                "giulia@example.com",
                "hashed-password",
                LocalDate.of(1998, 3, 20));

        userRepository.save(user);

        // Act
        boolean exists = userRepository.existsByEmailIgnoreCase(
                "GIULIA@EXAMPLE.COM");

        // Assert
        assertThat(exists).isTrue();
    }

    @Test
    void shouldReturnFalseWhenEmailDoesNotExist() {

        // Act
        boolean exists = userRepository.existsByEmailIgnoreCase(
                "unknown@example.com");

        // Assert
        assertThat(exists).isFalse();
    }

    @Test
    void shouldRejectDuplicateEmailIgnoringCase() {

        // Arrange
        User firstUser = new User(
                "Mario",
                "Rossi",
                "mario@example.com",
                "hashed-password",
                LocalDate.of(1995, 5, 15));

        User secondUser = new User(
                "Giulia",
                "Bianchi",
                "MARIO@EXAMPLE.COM",
                "hashed-password",
                LocalDate.of(1998, 3, 20));

        userRepository.saveAndFlush(firstUser);

        // Act & Assert
        assertThat(
                org.junit.jupiter.api.Assertions.assertThrows(
                        DataIntegrityViolationException.class,
                        () -> userRepository.saveAndFlush(secondUser)))
                .isNotNull();
    }
}
