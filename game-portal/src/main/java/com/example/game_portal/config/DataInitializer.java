package com.example.game_portal.config;

import com.example.game_portal.entity.Role;
import com.example.game_portal.entity.User;
import com.example.game_portal.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

// Runs once on startup — creates the hardcoded admin account if it doesn't exist.
// Credentials: username = admin  /  password = Admin@1234
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository  userRepository;
    private final PasswordEncoder passwordEncoder;

    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "Admin@1234";
    private static final String ADMIN_EMAIL    = "admin@gameportal.com";

    @Override
    public void run(String... args) {
        userRepository.findByUsername(ADMIN_USERNAME).ifPresentOrElse(
            existing -> {
                if (existing.getRole() != Role.ADMIN) {
                    existing.setRole(Role.ADMIN);
                    userRepository.save(existing);
                    log.info("Admin role restored for user '{}'", ADMIN_USERNAME);
                }
            },
            () -> {
                User admin = User.builder()
                        .username(ADMIN_USERNAME)
                        .email(ADMIN_EMAIL)
                        .password(passwordEncoder.encode(ADMIN_PASSWORD))
                        .role(Role.ADMIN)
                        .build();
                userRepository.save(admin);
                log.info("Admin account created — username: {} | password: {}", ADMIN_USERNAME, ADMIN_PASSWORD);
            }
        );
    }
}

/*
 * Without Spring, there is no CommandLineRunner. You would initialise the admin account
 * by running a SQL script manually before starting the application:
 *
 *   INSERT INTO users (username, email, password, role)
 *   VALUES ('admin', 'admin@gameportal.com', '<sha256-hash>', 'ADMIN');
 *
 * The problem is you would need to hash the password manually before inserting,
 * and re-run the script if the database is ever reset.
 *
 * With Spring, CommandLineRunner runs automatically after startup,
 * BCrypt hashes the password for you, and the account is recreated if missing.
 */
