package com.callinsights.gatewayservice.model.dto;

import java.util.List;

public record CallDetailResponse(
        String callId,
        String agentId,
        String templateId,
        String mediaPath,
        String timestamp,
        List<ArtifactDetailResponse> artifacts
) {
}
