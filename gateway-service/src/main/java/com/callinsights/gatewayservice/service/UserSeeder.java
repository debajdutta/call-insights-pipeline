package com.callinsights.gatewayservice.service;

import com.callinsights.gatewayservice.document.UserDocument;
import com.callinsights.gatewayservice.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Creates the first supervisor account on startup if the users collection is empty, so there's
 * always at least one login without a separate manual provisioning step. Anyone needing more
 * users beyond this one inserts directly into MongoDB for now (no user-management UI/API yet).
 */
@Component
public class UserSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(UserSeeder.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String seedUsername;
    private final String seedPassword;

    public UserSeeder(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       @Value("${gateway-service.seed-user.username}") String seedUsername,
                       @Value("${gateway-service.seed-user.password}") String seedPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.seedUsername = seedUsername;
        this.seedPassword = seedPassword;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }
        userRepository.save(new UserDocument(seedUsername, passwordEncoder.encode(seedPassword)));
        log.info("Seeded first supervisor user '{}' (users collection was empty). "
                + "Override via SEED_USER_USERNAME/SEED_USER_PASSWORD env vars for anything beyond local dev.",
                seedUsername);
    }
}
