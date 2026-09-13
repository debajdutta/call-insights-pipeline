package com.callinsights.gatewayservice.model.dto;

public record LoginResponse(String token, String username, long expiresInSeconds) {
}
