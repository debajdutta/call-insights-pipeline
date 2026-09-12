package com.callinsights.summaryservice.model;

public record ArtifactRegenerationRequestedEvent(
        String callId,
        String artifactType,
        String requestedModel,
        String requestedBy,
        String agentId,
        String templateId
) {
}
