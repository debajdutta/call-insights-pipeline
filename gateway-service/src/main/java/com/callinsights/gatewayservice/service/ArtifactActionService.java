package com.callinsights.gatewayservice.service;

import com.callinsights.gatewayservice.document.ArtifactCatalogDocument;
import com.callinsights.gatewayservice.document.ArtifactVersionEntry;
import com.callinsights.gatewayservice.document.CallDocument;
import com.callinsights.gatewayservice.model.ArtifactDeletedEvent;
import com.callinsights.gatewayservice.model.ArtifactRegenerationRequestedEvent;
import com.callinsights.gatewayservice.repository.ArtifactCatalogRepository;
import com.callinsights.gatewayservice.repository.CallRepository;
import com.callinsights.gatewayservice.web.InvalidRequestException;
import com.callinsights.gatewayservice.web.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class ArtifactActionService {

    private static final Logger log = LoggerFactory.getLogger(ArtifactActionService.class);
    private static final Set<String> VALID_ARTIFACT_TYPES = Set.of("transcript", "summary", "evaluation");

    private final CallRepository callRepository;
    private final ArtifactCatalogRepository artifactCatalogRepository;
    private final GatewayEventPublisher eventPublisher;

    public ArtifactActionService(CallRepository callRepository,
                                  ArtifactCatalogRepository artifactCatalogRepository,
                                  GatewayEventPublisher eventPublisher) {
        this.callRepository = callRepository;
        this.artifactCatalogRepository = artifactCatalogRepository;
        this.eventPublisher = eventPublisher;
    }

    public void regenerate(String callId, String artifactType, String requestedModel, String requestedBy) {
        validateArtifactType(artifactType);
        CallDocument call = callRepository.findById(callId)
                .orElseThrow(() -> new NotFoundException("No such call: " + callId));

        eventPublisher.publishRegenerationRequested(new ArtifactRegenerationRequestedEvent(
                callId, artifactType, requestedModel, requestedBy, call.getAgentId(), call.getTemplateId()));
    }

    public int delete(String callId, String artifactType, String deletedBy) {
        validateArtifactType(artifactType);
        String id = ArtifactCatalogDocument.buildId(callId, artifactType);
        ArtifactCatalogDocument catalogEntry = artifactCatalogRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("No " + artifactType + " artifact for call: " + callId));
        if (catalogEntry.isDeleted() || catalogEntry.getCurrentVersion() == null) {
            throw new NotFoundException("No " + artifactType + " artifact for call: " + callId);
        }

        int versionToDelete = catalogEntry.getCurrentVersion();
        List<ArtifactVersionEntry> versions = catalogEntry.getVersions();
        Optional<ArtifactVersionEntry> currentVersionEntry = versions.stream()
                .filter(v -> v.getVersion() == versionToDelete)
                .findFirst();
        currentVersionEntry.ifPresent(v -> deleteFile(v.getPath(), callId, artifactType));

        eventPublisher.publishArtifactDeleted(new ArtifactDeletedEvent(callId, artifactType, versionToDelete, deletedBy));
        return versionToDelete;
    }

    private void deleteFile(String path, String callId, String artifactType) {
        try {
            boolean deleted = Files.deleteIfExists(Path.of(path));
            if (!deleted) {
                log.warn("Artifact file already absent at {} for callId={} artifactType={}", path, callId, artifactType);
            }
        } catch (IOException e) {
            log.warn("Could not delete artifact file at {} for callId={} artifactType={}: {}",
                    path, callId, artifactType, e.getMessage());
        }
    }

    private void validateArtifactType(String artifactType) {
        if (!VALID_ARTIFACT_TYPES.contains(artifactType)) {
            throw new InvalidRequestException("Unknown artifact type: " + artifactType);
        }
    }
}
