package com.callinsights.evaluationservice.model;

import java.util.List;

public record EvaluationArtifact(
        String callId,
        String agentId,
        String templateId,
        int version,
        String modelUsed,
        String generatedAt,
        double score,
        String verdict,
        List<String> matchedKeywords,
        List<String> missingKeywords
) {
}
