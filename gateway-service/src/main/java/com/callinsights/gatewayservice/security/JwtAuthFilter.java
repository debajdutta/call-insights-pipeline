package com.callinsights.gatewayservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Hand-rolled bearer-token check rather than full Spring Security - this Gateway has exactly two
 * states (anonymous on /api/auth/login and /actuator/**, authenticated everywhere else under
 * /api/**), so a filter chain framework would add configuration surface without buying anything.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    public static final String AUTHENTICATED_USERNAME_ATTRIBUTE = "authenticatedUsername";

    private static final List<String> PUBLIC_PATH_PREFIXES = List.of("/api/auth/login", "/actuator");

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return PUBLIC_PATH_PREFIXES.stream().anyMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        String token = (authHeader != null && authHeader.startsWith("Bearer "))
                ? authHeader.substring("Bearer ".length())
                : null;

        Optional<String> username = token != null ? jwtService.extractUsername(token) : Optional.empty();
        if (username.isEmpty()) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing or invalid bearer token");
            return;
        }

        request.setAttribute(AUTHENTICATED_USERNAME_ATTRIBUTE, username.get());
        filterChain.doFilter(request, response);
    }
}
