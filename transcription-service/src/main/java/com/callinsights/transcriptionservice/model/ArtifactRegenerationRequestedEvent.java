package com.callinsights.transcriptionservice.model;

/**
 * agentId/templateId are not in SPEC.md's original draft of this topic's payload (§4.3) - added
 * as optional fields so a future Gateway/BFF (which already reads the Mongo catalog) can supply
 * richer regeneration context without another schema change. Until then they may be null, and
 * the orchestrator falls back to generic placeholders.
 */
public record ArtifactRegenerationRequestedEvent(
        String callId,
        String artifactType,
        String requestedModel,
        String requestedBy,
        String agentId,
        String templateId
) {
}
