package com.callinsights.common.event;

/**
 * Topic {@code artifact-regeneration-requested}. Produced by gateway-service; consumed by
 * transcription-service, summary-service and evaluation-service (each acts only on its own
 * {@code artifactType}).
 *
 * <p>{@code agentId}/{@code templateId} were added beyond SPEC.md's original draft so the
 * gateway can pass catalog context; they may be null, in which case consumers fall back to
 * the latest artifact's values or generic placeholders. evaluation-service ignores
 * {@code requestedModel} because its scoring is rule-based, not model-selectable.
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
