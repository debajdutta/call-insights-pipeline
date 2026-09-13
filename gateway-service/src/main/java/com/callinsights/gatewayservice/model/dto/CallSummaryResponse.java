package com.callinsights.gatewayservice.model.dto;

import java.util.List;

public record CallSummaryResponse(
        String callId,
        String agentId,
        String templateId,
        String timestamp,
        List<ArtifactSummaryResponse> artifacts
) {
}
