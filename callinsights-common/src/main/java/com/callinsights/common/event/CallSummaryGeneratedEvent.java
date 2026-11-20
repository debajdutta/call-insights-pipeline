package com.callinsights.common.event;

/**
 * Topic {@code call-summary-generated}. Produced by summary-service; consumed by catalog-service.
 */
public record CallSummaryGeneratedEvent(
        String callId,
        String agentId,
        String templateId,
        String summaryPath,
        int version,
        String modelUsed
) {
}
