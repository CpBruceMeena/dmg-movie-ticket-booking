package com.dmg.moviebooking.service;

import com.dmg.moviebooking.dto.request.LoginRequest;
import com.dmg.moviebooking.dto.response.AuthResponse;
import com.dmg.moviebooking.entity.User;
import com.dmg.moviebooking.enums.Role;
import com.dmg.moviebooking.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class LoginServiceTest {

    @Autowired
    private LoginService loginService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        // Create a test user for authentication
        if (!userRepository.existsByUsername("testuser")) {
            userRepository.save(User.builder()
                    .username("testuser")
                    .email("testuser@example.com")
                    .password(passwordEncoder.encode("password123"))
                    .fullName("Test User")
                    .role(Role.ROLE_CUSTOMER)
                    .active(true)
                    .build());
        }

        if (!userRepository.existsByUsername("admin")) {
            userRepository.save(User.builder()
                    .username("admin")
                    .email("admin@example.com")
                    .password(passwordEncoder.encode("admin123"))
                    .fullName("Admin User")
                    .role(Role.ROLE_ADMIN)
                    .active(true)
                    .build());
        }

        // Create a deactivated user for testing
        if (!userRepository.existsByUsername("deactivated")) {
            userRepository.save(User.builder()
                    .username("deactivated")
                    .email("deactivated@example.com")
                    .password(passwordEncoder.encode("password123"))
                    .fullName("Deactivated User")
                    .role(Role.ROLE_CUSTOMER)
                    .active(false)
                    .build());
        }
    }

    @Test
    void authenticate_WithValidCustomerCredentials_ShouldReturnToken() {
        AuthResponse response = loginService.authenticate(
                new LoginRequest("testuser", "password123"));

        assertNotNull(response);
        assertNotNull(response.getToken());
        assertEquals("testuser", response.getUsername());
        assertEquals("ROLE_CUSTOMER", response.getRole());
    }

    @Test
    void authenticate_WithValidAdminCredentials_ShouldReturnToken() {
        AuthResponse response = loginService.authenticate(
                new LoginRequest("admin", "admin123"));

        assertNotNull(response);
        assertNotNull(response.getToken());
        assertEquals("admin", response.getUsername());
        assertEquals("ROLE_ADMIN", response.getRole());
    }

    @Test
    void authenticate_WithInvalidUsername_ShouldThrowBadCredentials() {
        assertThrows(BadCredentialsException.class,
                () -> loginService.authenticate(new LoginRequest("nonexistent", "password123")));
    }

    @Test
    void authenticate_WithWrongPassword_ShouldThrowBadCredentials() {
        assertThrows(BadCredentialsException.class,
                () -> loginService.authenticate(new LoginRequest("testuser", "wrongpassword")));
    }

    @Test
    void authenticate_WithDeactivatedAccount_ShouldThrowBadCredentials() {
        assertThrows(BadCredentialsException.class,
                () -> loginService.authenticate(new LoginRequest("deactivated", "password123")));
    }
}
