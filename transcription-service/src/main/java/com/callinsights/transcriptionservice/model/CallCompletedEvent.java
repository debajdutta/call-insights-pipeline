package com.callinsights.transcriptionservice.model;

public record CallCompletedEvent(
        String callId,
        String agentId,
        String templateId,
        String mediaPath,
        String timestamp
) {
}
