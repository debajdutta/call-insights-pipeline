package com.callinsights.summaryservice.model;

public record SummaryArtifact(
        String callId,
        String agentId,
        String templateId,
        int version,
        String modelUsed,
        String generatedAt,
        String summaryText
) {
}
