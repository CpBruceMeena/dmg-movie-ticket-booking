package com.dmg.moviebooking.service;

import com.dmg.moviebooking.dto.request.LoginRequest;
import com.dmg.moviebooking.dto.response.AuthResponse;
import com.dmg.moviebooking.entity.User;
import com.dmg.moviebooking.repository.UserRepository;
import com.dmg.moviebooking.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LoginService {

    private static final Logger log = LoggerFactory.getLogger(LoginService.class);

    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    public LoginService(JwtTokenProvider jwtTokenProvider,
                        PasswordEncoder passwordEncoder,
                        UserRepository userRepository) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
    }

    public AuthResponse authenticate(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));

        if (!user.isActive()) {
            throw new BadCredentialsException("Account is deactivated");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Invalid username or password");
        }

        List<String> roles = List.of(user.getRole().name());
        String token = jwtTokenProvider.generateToken(user.getUsername(), roles);
        log.info("User '{}' authenticated successfully with role: {}", user.getUsername(), user.getRole());

        return AuthResponse.builder()
                .token(token)
                .username(user.getUsername())
                .role(user.getRole().name())
                .build();
    }
}
