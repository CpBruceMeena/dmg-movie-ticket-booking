package com.dmg.moviebooking.controller;

import com.dmg.moviebooking.dto.request.LoginRequest;
import com.dmg.moviebooking.dto.request.RegisterRequest;
import com.dmg.moviebooking.dto.response.AuthResponse;
import com.dmg.moviebooking.dto.response.UserResponse;
import com.dmg.moviebooking.service.LoginService;
import com.dmg.moviebooking.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Authentication endpoints for login, registration, and token management")
@SecurityRequirements({}) // Public endpoints - no JWT required
public class AuthController {

    private final LoginService loginService;
    private final UserService userService;

    public AuthController(LoginService loginService, UserService userService) {
        this.loginService = loginService;
        this.userService = userService;
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user and return JWT token")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = loginService.authenticate(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user account")
    @ApiResponse(responseCode = "201", description = "User registered successfully")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse response = userService.registerUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
