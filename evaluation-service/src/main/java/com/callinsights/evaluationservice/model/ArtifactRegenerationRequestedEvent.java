package com.callinsights.evaluationservice.model;

/**
 * requestedModel is carried for schema consistency with the other artifact types but is ignored
 * here - evaluation is rule-based keyword scoring (SPEC.md's non-ML requirement), not
 * model-selectable, so regenerating it always re-runs the currently configured ruleset.
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
