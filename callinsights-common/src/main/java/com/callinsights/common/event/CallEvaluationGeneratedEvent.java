package com.callinsights.common.event;

/**
 * Topic {@code call-evaluation-generated}. Produced by evaluation-service; consumed by
 * catalog-service.
 */
public record CallEvaluationGeneratedEvent(
        String callId,
        String agentId,
        String templateId,
        String evaluationPath,
        int version,
        String modelUsed
) {
}
