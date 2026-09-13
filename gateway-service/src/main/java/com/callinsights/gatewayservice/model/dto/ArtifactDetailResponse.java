package com.callinsights.gatewayservice.model.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

public record ArtifactDetailResponse(
        String artifactType,
        Integer currentVersion,
        boolean deleted,
        List<ArtifactVersionResponse> versions,
        JsonNode currentContent
) {
}
