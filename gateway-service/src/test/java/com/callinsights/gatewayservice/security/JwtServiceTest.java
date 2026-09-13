package com.callinsights.gatewayservice.security;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private final JwtService jwtService = new JwtService(
            "test-secret-key-at-least-32-bytes-long-for-hs256", 3_600_000L);

    @Test
    void generateToken_thenExtractUsername_roundTrips() {
        String token = jwtService.generateToken("supervisor");

        Optional<String> username = jwtService.extractUsername(token);

        assertThat(username).contains("supervisor");
    }

    @Test
    void extractUsername_returnsEmptyForGarbageToken() {
        assertThat(jwtService.extractUsername("not-a-real-token")).isEmpty();
    }

    @Test
    void extractUsername_returnsEmptyForTokenSignedWithDifferentKey() {
        JwtService otherService = new JwtService(
                "different-secret-key-at-least-32-bytes-long-here", 3_600_000L);
        String token = otherService.generateToken("supervisor");

        assertThat(jwtService.extractUsername(token)).isEmpty();
    }

    @Test
    void extractUsername_returnsEmptyForExpiredToken() throws InterruptedException {
        JwtService shortLivedService = new JwtService(
                "test-secret-key-at-least-32-bytes-long-for-hs256", 1L);
        String token = shortLivedService.generateToken("supervisor");
        Thread.sleep(10);

        assertThat(shortLivedService.extractUsername(token)).isEmpty();
    }
}
