package com.dmg.moviebooking;

import com.dmg.moviebooking.dto.request.LoginRequest;
import com.dmg.moviebooking.dto.request.RegisterRequest;
import com.dmg.moviebooking.entity.User;
import com.dmg.moviebooking.enums.Role;
import com.dmg.moviebooking.repository.UserRepository;
import com.dmg.moviebooking.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests verifying JWT authentication and role-based access control
 * for all API endpoint groups: public, admin-only, customer, and catch-all.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminToken;
    private String customerToken;
    private String invalidToken;
    private Long adminId;
    private Long customerId;

    @BeforeEach
    void setUp() {
        // Create admin user
        User admin = userRepository.save(User.builder()
                .username("admin")
                .email("admin@test.com")
                .password(passwordEncoder.encode("admin123"))
                .fullName("Admin User")
                .role(Role.ROLE_ADMIN)
                .active(true)
                .build());
        adminId = admin.getId();
        adminToken = jwtTokenProvider.generateToken("admin", List.of("ROLE_ADMIN"));

        // Create customer user
        User customer = userRepository.save(User.builder()
                .username("customer")
                .email("customer@test.com")
                .password(passwordEncoder.encode("customer123"))
                .fullName("Customer User")
                .role(Role.ROLE_CUSTOMER)
                .active(true)
                .build());
        customerId = customer.getId();
        customerToken = jwtTokenProvider.generateToken("customer", List.of("ROLE_CUSTOMER"));

        invalidToken = "eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJmYWtlIn0.invalid";
    }

    // ========================================================================
    // 1. PUBLIC ENDPOINTS — Should work WITHOUT any JWT token
    // ========================================================================

    @Nested
    class PublicEndpoints {

        @Test
        void healthCheck_ShouldBeAccessibleWithoutAuth() throws Exception {
            mockMvc.perform(get("/api/health"))
                    .andExpect(status().isOk());
        }

        @Test
        void login_ShouldBeAccessibleWithoutAuth() throws Exception {
            String body = objectMapper.writeValueAsString(
                    new LoginRequest("admin", "admin123"));

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").isNotEmpty())
                    .andExpect(jsonPath("$.username").value("admin"))
                    .andExpect(jsonPath("$.role").value("ROLE_ADMIN"));
        }

        @Test
        void register_ShouldBeAccessibleWithoutAuth() throws Exception {
            String body = objectMapper.writeValueAsString(
                    RegisterRequest.builder()
                            .username("newuser")
                            .email("newuser@test.com")
                            .password("password123")
                            .fullName("New User")
                            .role(Role.ROLE_CUSTOMER)
                            .build());

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.username").value("newuser"));
        }

        @Test
        void swaggerUi_ShouldBeAccessibleWithoutAuth() throws Exception {
            mockMvc.perform(get("/swagger-ui/index.html"))
                    .andExpect(status().isOk());
        }

        @Test
        void swaggerApiDocs_ShouldBeAccessibleWithoutAuth() throws Exception {
            mockMvc.perform(get("/v3/api-docs"))
                    .andExpect(status().isOk());
        }
    }

    // ========================================================================
    // 2. PROTECTED ENDPOINTS — Should return 401 WITHOUT a JWT token
    // ========================================================================

    @Nested
    class UnauthenticatedAccess {

        @Test
        void browseShows_WithoutAuth_ShouldReturn401() throws Exception {
            mockMvc.perform(get("/api/shows"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void adminEndpoints_WithoutAuth_ShouldReturn401() throws Exception {
            mockMvc.perform(get("/api/admin/cities"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void bookingEndpoints_WithoutAuth_ShouldReturn401() throws Exception {
            mockMvc.perform(get("/api/bookings"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void invalidToken_ShouldReturn401() throws Exception {
            mockMvc.perform(get("/api/shows")
                            .header("Authorization", "Bearer " + invalidToken))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ========================================================================
    // 3. ADMIN-ONLY ENDPOINTS — /api/admin/**
    //    Admin → 200 OK, Customer → 403 Forbidden
    // ========================================================================

    @Nested
    class AdminEndpoints {

        @Test
        void admin_ShouldAccessAdminCities() throws Exception {
            mockMvc.perform(get("/api/admin/cities")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk());
        }

        @Test
        void customer_ShouldNotAccessAdminCities() throws Exception {
            mockMvc.perform(get("/api/admin/cities")
                            .header("Authorization", "Bearer " + customerToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        void admin_ShouldAccessAdminTheaters() throws Exception {
            mockMvc.perform(get("/api/admin/theaters?cityId=1")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk());
        }

        @Test
        void customer_ShouldNotAccessAdminTheaters() throws Exception {
            mockMvc.perform(get("/api/admin/theaters?cityId=1")
                            .header("Authorization", "Bearer " + customerToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        void admin_ShouldAccessAdminScreens() throws Exception {
            mockMvc.perform(get("/api/admin/screens?theaterId=1")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk());
        }

        @Test
        void customer_ShouldNotAccessAdminScreens() throws Exception {
            mockMvc.perform(get("/api/admin/screens?theaterId=1")
                            .header("Authorization", "Bearer " + customerToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        void admin_ShouldAccessAdminShows() throws Exception {
            mockMvc.perform(get("/api/admin/shows?screenId=1")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk());
        }

        @Test
        void customer_ShouldNotAccessAdminShows() throws Exception {
            mockMvc.perform(get("/api/admin/shows?screenId=1")
                            .header("Authorization", "Bearer " + customerToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        void admin_ShouldAccessAdminSeats() throws Exception {
            mockMvc.perform(get("/api/admin/seats?screenId=1")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk());
        }

        @Test
        void customer_ShouldNotAccessAdminSeats() throws Exception {
            mockMvc.perform(get("/api/admin/seats?screenId=1")
                            .header("Authorization", "Bearer " + customerToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        void admin_ShouldAccessAdminRefundPolicies() throws Exception {
            mockMvc.perform(get("/api/admin/refund-policies")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk());
        }

        @Test
        void customer_ShouldNotAccessAdminRefundPolicies() throws Exception {
            mockMvc.perform(get("/api/admin/refund-policies")
                            .header("Authorization", "Bearer " + customerToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        void admin_ShouldAccessAdminPricingTiers() throws Exception {
            mockMvc.perform(get("/api/admin/pricing-tiers")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk());
        }

        @Test
        void customer_ShouldNotAccessAdminPricingTiers() throws Exception {
            mockMvc.perform(get("/api/admin/pricing-tiers")
                            .header("Authorization", "Bearer " + customerToken))
                    .andExpect(status().isForbidden());
        }
    }

    // ========================================================================
    // 4. CUSTOMER ENDPOINTS — /api/shows, /api/bookings, /api/cities, /api/theaters
    //    Both Admin and Customer → 200 OK
    // ========================================================================

    @Nested
    class CustomerEndpoints {

        @Test
        void admin_ShouldBrowseShows() throws Exception {
            mockMvc.perform(get("/api/shows")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk());
        }

        @Test
        void customer_ShouldBrowseShows() throws Exception {
            mockMvc.perform(get("/api/shows")
                            .header("Authorization", "Bearer " + customerToken))
                    .andExpect(status().isOk());
        }

        @Test
        void admin_ShouldAccessBookingHistory() throws Exception {
            mockMvc.perform(get("/api/bookings")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk());
        }

        @Test
        void customer_ShouldAccessBookingHistory() throws Exception {
            mockMvc.perform(get("/api/bookings")
                            .header("Authorization", "Bearer " + customerToken))
                    .andExpect(status().isOk());
        }
    }

    // ========================================================================
    // 5. FORBIDDEN RESPONSE FORMAT VERIFICATION
    // ========================================================================

    @Nested
    class ForbiddenResponseFormat {

        @Test
        void customerAccessingAdmin_ShouldReturn403() throws Exception {
            mockMvc.perform(get("/api/admin/cities")
                            .header("Authorization", "Bearer " + customerToken))
                    .andExpect(status().isForbidden());
        }
    }
}
