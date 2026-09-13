package com.callinsights.gatewayservice.model.dto;

public record AuditEntryResponse(
        String callId,
        String artifactType,
        String action,
        Integer version,
        String modelUsed,
        String actor,
        String timestamp
) {
}
