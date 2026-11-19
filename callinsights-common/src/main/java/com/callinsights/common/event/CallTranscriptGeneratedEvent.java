package com.callinsights.common.event;

/**
 * Topic {@code call-transcript-generated}. Produced by transcription-service; consumed by
 * summary-service, evaluation-service and catalog-service.
 */
public record CallTranscriptGeneratedEvent(
        String callId,
        String agentId,
        String templateId,
        String transcriptPath,
        int version,
        String modelUsed
) {
}
