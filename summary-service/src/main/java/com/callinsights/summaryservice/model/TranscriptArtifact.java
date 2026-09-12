package com.callinsights.summaryservice.model;

/**
 * Read-side copy of transcription-service's transcript_v{n}.json shape - this service only
 * ever deserializes these files, never writes them.
 */
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
