package com.dmg.moviebooking.service;

import com.dmg.moviebooking.dto.request.LoginRequest;
import com.dmg.moviebooking.dto.response.AuthResponse;
import com.dmg.moviebooking.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginService {

    private static final Logger log = LoggerFactory.getLogger(LoginService.class);

    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final Map<String, UserDetails> users = new ConcurrentHashMap<>();

    public LoginService(JwtTokenProvider jwtTokenProvider, PasswordEncoder passwordEncoder) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordEncoder = passwordEncoder;
        initDefaultUsers();
    }

    public AuthResponse authenticate(LoginRequest request) {
        UserDetails user = users.get(request.getUsername());
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.password)) {
            throw new BadCredentialsException("Invalid username or password");
        }

        String token = jwtTokenProvider.generateToken(user.username, user.roles);
        log.info("User '{}' authenticated successfully with roles: {}", user.username, user.roles);

        return AuthResponse.builder()
                .token(token)
                .username(user.username)
                .role(user.roles.get(0))
                .build();
    }

    private void initDefaultUsers() {
        users.put("admin", new UserDetails("admin",
                passwordEncoder.encode("admin123"),
                List.of("ROLE_ADMIN")));
        users.put("customer", new UserDetails("customer",
                passwordEncoder.encode("customer123"),
                List.of("ROLE_CUSTOMER")));
    }

    private record UserDetails(String username, String password, List<String> roles) {}
}
