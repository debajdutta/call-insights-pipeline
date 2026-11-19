package com.callinsights.common.event;

/**
 * Topic {@code artifact-deleted}. Produced by gateway-service after it removes the artifact
 * file; consumed by catalog-service.
 */
public record ArtifactDeletedEvent(
        String callId,
        String artifactType,
        int version,
        String deletedBy
) {
}
