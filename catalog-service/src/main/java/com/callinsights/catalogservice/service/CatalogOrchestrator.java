package com.callinsights.catalogservice.service;

import com.callinsights.catalogservice.document.ArtifactCatalogDocument;
import com.callinsights.catalogservice.document.ArtifactVersionEntry;
import com.callinsights.catalogservice.document.AuditLogDocument;
import com.callinsights.catalogservice.document.CallDocument;
import com.callinsights.catalogservice.model.ArtifactDeletedEvent;
import com.callinsights.catalogservice.model.CallCompletedEvent;
import com.callinsights.catalogservice.model.CallEvaluationGeneratedEvent;
import com.callinsights.catalogservice.model.CallSummaryGeneratedEvent;
import com.callinsights.catalogservice.model.CallTranscriptGeneratedEvent;
import com.callinsights.catalogservice.repository.ArtifactCatalogRepository;
import com.callinsights.catalogservice.repository.AuditLogRepository;
import com.callinsights.catalogservice.repository.CallRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class CatalogOrchestrator {

    private static final String ACTION_CALL_CREATED = "call_created";
    private static final String ACTION_ARTIFACT_GENERATED = "artifact_generated";
    private static final String ACTION_ARTIFACT_DELETED = "artifact_deleted";
    private static final String ACTOR_SYSTEM = "system";

    private final CallRepository callRepository;
    private final ArtifactCatalogRepository artifactCatalogRepository;
    private final AuditLogRepository auditLogRepository;

    public CatalogOrchestrator(CallRepository callRepository,
                                ArtifactCatalogRepository artifactCatalogRepository,
                                AuditLogRepository auditLogRepository) {
        this.callRepository = callRepository;
        this.artifactCatalogRepository = artifactCatalogRepository;
        this.auditLogRepository = auditLogRepository;
    }

    public void handleCallCompleted(CallCompletedEvent event) {
        CallDocument call = new CallDocument(
                event.callId(), event.agentId(), event.templateId(), event.mediaPath(), event.timestamp());
        callRepository.save(call);
        auditLogRepository.save(new AuditLogDocument(
                event.callId(), null, ACTION_CALL_CREATED, null, null, ACTOR_SYSTEM, Instant.now().toString()));
    }

    public void handleTranscriptGenerated(CallTranscriptGeneratedEvent event) {
        recordArtifact(event.callId(), "transcript", event.transcriptPath(), event.version(), event.modelUsed());
    }

    public void handleSummaryGenerated(CallSummaryGeneratedEvent event) {
        recordArtifact(event.callId(), "summary", event.summaryPath(), event.version(), event.modelUsed());
    }

    public void handleEvaluationGenerated(CallEvaluationGeneratedEvent event) {
        recordArtifact(event.callId(), "evaluation", event.evaluationPath(), event.version(), event.modelUsed());
    }

    public void handleArtifactDeleted(ArtifactDeletedEvent event) {
        String id = ArtifactCatalogDocument.buildId(event.callId(), event.artifactType());
        ArtifactCatalogDocument catalogEntry = artifactCatalogRepository.findById(id).orElse(null);
        if (catalogEntry != null) {
            catalogEntry.setDeleted(true);
            artifactCatalogRepository.save(catalogEntry);
        }
        auditLogRepository.save(new AuditLogDocument(
                event.callId(), event.artifactType(), ACTION_ARTIFACT_DELETED, event.version(),
                null, event.deletedBy(), Instant.now().toString()));
    }

    private void recordArtifact(String callId, String artifactType, String path, int version, String modelUsed) {
        String id = ArtifactCatalogDocument.buildId(callId, artifactType);
        ArtifactCatalogDocument catalogEntry = artifactCatalogRepository.findById(id).orElseGet(() -> {
            ArtifactCatalogDocument fresh = new ArtifactCatalogDocument();
            fresh.setId(id);
            fresh.setCallId(callId);
            fresh.setArtifactType(artifactType);
            return fresh;
        });

        String generatedAt = Instant.now().toString();
        catalogEntry.getVersions().add(new ArtifactVersionEntry(version, path, modelUsed, generatedAt));
        catalogEntry.setCurrentVersion(version);
        catalogEntry.setDeleted(false);
        artifactCatalogRepository.save(catalogEntry);

        auditLogRepository.save(new AuditLogDocument(
                callId, artifactType, ACTION_ARTIFACT_GENERATED, version, modelUsed, ACTOR_SYSTEM, generatedAt));
    }
}
