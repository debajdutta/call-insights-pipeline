package com.callinsights.catalogservice.model;

public record CallTranscriptGeneratedEvent(
        String callId,
        String agentId,
        String templateId,
        String transcriptPath,
        int version,
        String modelUsed
) {
}
