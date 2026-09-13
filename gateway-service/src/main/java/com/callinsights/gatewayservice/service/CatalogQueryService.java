package com.callinsights.gatewayservice.service;

import com.callinsights.gatewayservice.document.ArtifactCatalogDocument;
import com.callinsights.gatewayservice.document.ArtifactVersionEntry;
import com.callinsights.gatewayservice.document.AuditLogDocument;
import com.callinsights.gatewayservice.document.CallDocument;
import com.callinsights.gatewayservice.model.dto.ArtifactDetailResponse;
import com.callinsights.gatewayservice.model.dto.ArtifactSummaryResponse;
import com.callinsights.gatewayservice.model.dto.ArtifactVersionResponse;
import com.callinsights.gatewayservice.model.dto.AuditEntryResponse;
import com.callinsights.gatewayservice.model.dto.CallDetailResponse;
import com.callinsights.gatewayservice.model.dto.CallSummaryResponse;
import com.callinsights.gatewayservice.repository.ArtifactCatalogRepository;
import com.callinsights.gatewayservice.repository.AuditLogRepository;
import com.callinsights.gatewayservice.repository.CallRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

@Service
public class CatalogQueryService {

    private static final Logger log = LoggerFactory.getLogger(CatalogQueryService.class);

    private final CallRepository callRepository;
    private final ArtifactCatalogRepository artifactCatalogRepository;
    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public CatalogQueryService(CallRepository callRepository,
                                ArtifactCatalogRepository artifactCatalogRepository,
                                AuditLogRepository auditLogRepository,
                                ObjectMapper objectMapper) {
        this.callRepository = callRepository;
        this.artifactCatalogRepository = artifactCatalogRepository;
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    public List<CallSummaryResponse> listCalls() {
        return callRepository.findAll().stream()
                .map(call -> new CallSummaryResponse(
                        call.getCallId(), call.getAgentId(), call.getTemplateId(), call.getTimestamp(),
                        artifactCatalogRepository.findByCallId(call.getCallId()).stream()
                                .map(a -> new ArtifactSummaryResponse(a.getArtifactType(), a.getCurrentVersion(), a.isDeleted()))
                                .toList()))
                .toList();
    }

    public Optional<CallDetailResponse> getCallDetail(String callId) {
        CallDocument call = callRepository.findById(callId).orElse(null);
        if (call == null) {
            return Optional.empty();
        }
        List<ArtifactDetailResponse> artifacts = artifactCatalogRepository.findByCallId(callId).stream()
                .map(this::toArtifactDetail)
                .toList();
        return Optional.of(new CallDetailResponse(
                call.getCallId(), call.getAgentId(), call.getTemplateId(), call.getMediaPath(), call.getTimestamp(), artifacts));
    }

    public List<AuditEntryResponse> getAuditLog(String callId) {
        return auditLogRepository.findByCallIdOrderByTimestampDesc(callId).stream()
                .map(a -> new AuditEntryResponse(
                        a.getCallId(), a.getArtifactType(), a.getAction(), a.getVersion(), a.getModelUsed(), a.getActor(), a.getTimestamp()))
                .toList();
    }

    private ArtifactDetailResponse toArtifactDetail(ArtifactCatalogDocument catalogEntry) {
        List<ArtifactVersionResponse> versions = catalogEntry.getVersions().stream()
                .map(v -> new ArtifactVersionResponse(v.getVersion(), v.getPath(), v.getModelUsed(), v.getGeneratedAt()))
                .toList();

        JsonNode currentContent = null;
        if (!catalogEntry.isDeleted() && catalogEntry.getCurrentVersion() != null) {
            currentContent = readCurrentVersionContent(catalogEntry).orElse(null);
        }

        return new ArtifactDetailResponse(
                catalogEntry.getArtifactType(), catalogEntry.getCurrentVersion(), catalogEntry.isDeleted(), versions, currentContent);
    }

    private Optional<JsonNode> readCurrentVersionContent(ArtifactCatalogDocument catalogEntry) {
        Optional<ArtifactVersionEntry> currentVersionEntry = catalogEntry.getVersions().stream()
                .filter(v -> v.getVersion() == catalogEntry.getCurrentVersion())
                .findFirst();
        if (currentVersionEntry.isEmpty()) {
            return Optional.empty();
        }
        try {
            String raw = Files.readString(Path.of(currentVersionEntry.get().getPath()));
            return Optional.of(objectMapper.readTree(raw));
        } catch (IOException e) {
            log.warn("Could not read artifact content at {} for callId={} artifactType={}: {}",
                    currentVersionEntry.get().getPath(), catalogEntry.getCallId(), catalogEntry.getArtifactType(), e.getMessage());
            return Optional.empty();
        }
    }
}
