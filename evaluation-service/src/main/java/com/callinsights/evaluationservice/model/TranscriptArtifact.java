package com.callinsights.evaluationservice.model;

public record TranscriptArtifact(
        String callId,
        String agentId,
        String templateId,
        int version,
        String modelUsed,
        String generatedAt,
        String transcriptText
) {
}
