package com.callinsights.summaryservice.model;

public record CallSummaryGeneratedEvent(
        String callId,
        String agentId,
        String templateId,
        String summaryPath,
        int version,
        String modelUsed
) {
}
