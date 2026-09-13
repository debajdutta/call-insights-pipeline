package com.callinsights.gatewayservice.model;

public record ArtifactDeletedEvent(
        String callId,
        String artifactType,
        int version,
        String deletedBy
) {
}
