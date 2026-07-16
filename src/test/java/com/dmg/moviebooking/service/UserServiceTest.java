package com.dmg.moviebooking.service;

import com.dmg.moviebooking.dto.request.RegisterRequest;
import com.dmg.moviebooking.dto.response.UserResponse;
import com.dmg.moviebooking.entity.User;
import com.dmg.moviebooking.enums.Role;
import com.dmg.moviebooking.exception.DuplicateResourceException;
import com.dmg.moviebooking.exception.ResourceNotFoundException;
import com.dmg.moviebooking.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        // Create a test user for reference
        if (!userRepository.existsByUsername("existinguser")) {
            userRepository.save(User.builder()
                    .username("existinguser")
                    .email("existing@example.com")
                    .password(passwordEncoder.encode("password123"))
                    .fullName("Existing User")
                    .role(Role.ROLE_CUSTOMER)
                    .active(true)
                    .build());
        }
    }

    @Test
    void registerUser_ShouldCreateNewCustomerUser() {
        RegisterRequest request = RegisterRequest.builder()
                .username("newcustomer")
                .email("newcustomer@example.com")
                .password("password123")
                .fullName("New Customer")
                .role(Role.ROLE_CUSTOMER)
                .build();

        UserResponse response = userService.registerUser(request);

        assertNotNull(response.getId());
        assertEquals("newcustomer", response.getUsername());
        assertEquals("newcustomer@example.com", response.getEmail());
        assertEquals("New Customer", response.getFullName());
        assertEquals(Role.ROLE_CUSTOMER, response.getRole());
        assertTrue(response.isActive());
    }

    @Test
    void registerUser_ShouldCreateNewAdminUser() {
        RegisterRequest request = RegisterRequest.builder()
                .username("newadmin")
                .email("newadmin@example.com")
                .password("adminpass123")
                .fullName("New Admin")
                .role(Role.ROLE_ADMIN)
                .build();

        UserResponse response = userService.registerUser(request);

        assertEquals(Role.ROLE_ADMIN, response.getRole());
        assertTrue(response.isActive());
    }

    @Test
    void registerUser_ShouldThrow_WhenUsernameTaken() {
        RegisterRequest request = RegisterRequest.builder()
                .username("existinguser")
                .email("different@example.com")
                .password("password123")
                .fullName("Different Name")
                .build();

        assertThrows(DuplicateResourceException.class,
                () -> userService.registerUser(request));
    }

    @Test
    void registerUser_ShouldThrow_WhenEmailTaken() {
        RegisterRequest request = RegisterRequest.builder()
                .username("differentuser")
                .email("existing@example.com")
                .password("password123")
                .fullName("Different User")
                .build();

        assertThrows(DuplicateResourceException.class,
                () -> userService.registerUser(request));
    }

    @Test
    void getUserById_ShouldReturnUser() {
        // Create a user first, then fetch by its ID
        UserResponse created = userService.registerUser(RegisterRequest.builder()
                .username("findbyid")
                .email("findbyid@example.com")
                .password("password123")
                .fullName("Find By ID")
                .build());

        UserResponse response = userService.getUserById(created.getId());

        assertNotNull(response);
        assertEquals("findbyid", response.getUsername());
        assertEquals("Find By ID", response.getFullName());
    }

    @Test
    void getUserById_ShouldThrow_WhenNotFound() {
        assertThrows(ResourceNotFoundException.class,
                () -> userService.getUserById(999L));
    }

    @Test
    void getAllUsers_ShouldReturnAllUsers() {
        // Create an additional user
        userService.registerUser(RegisterRequest.builder()
                .username("anotheruser")
                .email("another@example.com")
                .password("password123")
                .fullName("Another User")
                .build());

        List<UserResponse> users = userService.getAllUsers();

        assertTrue(users.size() >= 2);
    }

    @Test
    void deactivateUser_ShouldSetActiveToFalse() {
        // Create a user to deactivate
        UserResponse created = userService.registerUser(RegisterRequest.builder()
                .username("todeactivate")
                .email("todeactivate@example.com")
                .password("password123")
                .fullName("To Deactivate")
                .build());

        userService.deactivateUser(created.getId());

        UserResponse deactivated = userService.getUserById(created.getId());
        assertFalse(deactivated.isActive());
    }
}
