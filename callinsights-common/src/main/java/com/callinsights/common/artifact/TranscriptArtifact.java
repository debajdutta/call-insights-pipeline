package com.callinsights.common.artifact;

/**
 * The transcript_v{n}.json file format. transcription-service writes it next to the call media;
 * summary-service and evaluation-service read it. This is a file format, not a Kafka event, but
 * the JSON field names are pinned the same way (see TranscriptArtifactJsonTest).
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
