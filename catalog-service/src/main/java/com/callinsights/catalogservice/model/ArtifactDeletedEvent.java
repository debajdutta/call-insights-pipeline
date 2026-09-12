package com.callinsights.catalogservice.model;

/**
 * Not yet published by anything - Gateway/BFF (Task 6) will publish this once delete is wired up.
 * The listener is added now so Task 6 only has to produce the event, not touch this consumer.
 */
public record ArtifactDeletedEvent(
        String callId,
        String artifactType,
        int version,
        String deletedBy
) {
}
