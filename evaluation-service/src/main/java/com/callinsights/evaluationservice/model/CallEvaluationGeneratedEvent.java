package com.callinsights.evaluationservice.model;

public record CallEvaluationGeneratedEvent(
        String callId,
        String agentId,
        String templateId,
        String evaluationPath,
        int version,
        String modelUsed
) {
}
