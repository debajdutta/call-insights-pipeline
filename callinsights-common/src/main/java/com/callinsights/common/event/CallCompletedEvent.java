package com.callinsights.common.event;

/**
 * Topic {@code call-completed}. Produced by call-generator; consumed by transcription-service
 * and catalog-service. {@code timestamp} is an ISO-8601 string.
 */
public record CallCompletedEvent(
        String callId,
        String agentId,
        String templateId,
        String mediaPath,
        String timestamp
) {
}
