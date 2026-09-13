package com.callinsights.gatewayservice.model.dto;

public record ArtifactVersionResponse(int version, String path, String modelUsed, String generatedAt) {
}
