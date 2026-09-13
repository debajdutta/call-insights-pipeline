package com.callinsights.gatewayservice.model.dto;

public record ArtifactSummaryResponse(String artifactType, Integer currentVersion, boolean deleted) {
}
