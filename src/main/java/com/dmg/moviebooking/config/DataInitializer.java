package com.dmg.moviebooking.config;

import com.dmg.moviebooking.entity.User;
import com.dmg.moviebooking.enums.Role;
import com.dmg.moviebooking.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (!userRepository.existsByUsername("admin")) {
            User admin = User.builder()
                    .username("admin")
                    .email("admin@dmg.com")
                    .password(passwordEncoder.encode("admin123"))
                    .fullName("System Administrator")
                    .role(Role.ROLE_ADMIN)
                    .active(true)
                    .build();
            userRepository.save(admin);
            log.info("Default admin user created (username: admin, password: admin123)");
        }

        if (!userRepository.existsByUsername("customer")) {
            User customer = User.builder()
                    .username("customer")
                    .email("customer@dmg.com")
                    .password(passwordEncoder.encode("customer123"))
                    .fullName("Default Customer")
                    .role(Role.ROLE_CUSTOMER)
                    .active(true)
                    .build();
            userRepository.save(customer);
            log.info("Default customer user created (username: customer, password: customer123)");
        }
    }
}
