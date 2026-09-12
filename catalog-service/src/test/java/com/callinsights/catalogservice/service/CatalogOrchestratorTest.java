package com.callinsights.catalogservice.service;

import com.callinsights.catalogservice.document.ArtifactCatalogDocument;
import com.callinsights.catalogservice.document.CallDocument;
import com.callinsights.catalogservice.model.CallCompletedEvent;
import com.callinsights.catalogservice.model.CallTranscriptGeneratedEvent;
import com.callinsights.catalogservice.repository.ArtifactCatalogRepository;
import com.callinsights.catalogservice.repository.AuditLogRepository;
import com.callinsights.catalogservice.repository.CallRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CatalogOrchestratorTest {

    private final CallRepository callRepository = mock(CallRepository.class);
    private final ArtifactCatalogRepository artifactCatalogRepository = mock(ArtifactCatalogRepository.class);
    private final AuditLogRepository auditLogRepository = mock(AuditLogRepository.class);
    private final CatalogOrchestrator orchestrator =
            new CatalogOrchestrator(callRepository, artifactCatalogRepository, auditLogRepository);

    @Test
    void handleCallCompleted_savesCallAndAuditEntry() {
        CallCompletedEvent event = new CallCompletedEvent(
                "call-1", "agent-001", "TEMPLATE_SALES_CALL", "/media/call-1", "2026-01-01T00:00:00Z");

        orchestrator.handleCallCompleted(event);

        verify(callRepository).save(any(CallDocument.class));
        verify(auditLogRepository).save(any());
    }

    @Test
    void handleTranscriptGenerated_createsNewCatalogEntryWithFirstVersion() {
        when(artifactCatalogRepository.findById("call-1:transcript")).thenReturn(Optional.empty());

        CallTranscriptGeneratedEvent event = new CallTranscriptGeneratedEvent(
                "call-1", "agent-001", "TEMPLATE_SALES_CALL", "/media/call-1/transcript_v1.json", 1, "claude-sonnet-5");

        orchestrator.handleTranscriptGenerated(event);

        var captor = org.mockito.ArgumentCaptor.forClass(ArtifactCatalogDocument.class);
        verify(artifactCatalogRepository).save(captor.capture());
        ArtifactCatalogDocument saved = captor.getValue();
        assertThat(saved.getCallId()).isEqualTo("call-1");
        assertThat(saved.getArtifactType()).isEqualTo("transcript");
        assertThat(saved.getCurrentVersion()).isEqualTo(1);
        assertThat(saved.getVersions()).hasSize(1);
        assertThat(saved.isDeleted()).isFalse();
    }

    @Test
    void handleTranscriptGenerated_appendsVersionToExistingCatalogEntry() {
        ArtifactCatalogDocument existing = new ArtifactCatalogDocument();
        existing.setId("call-1:transcript");
        existing.setCallId("call-1");
        existing.setArtifactType("transcript");
        existing.setDeleted(true);
        when(artifactCatalogRepository.findById("call-1:transcript")).thenReturn(Optional.of(existing));

        CallTranscriptGeneratedEvent event = new CallTranscriptGeneratedEvent(
                "call-1", "agent-001", "TEMPLATE_SALES_CALL", "/media/call-1/transcript_v2.json", 2, "claude-sonnet-5");

        orchestrator.handleTranscriptGenerated(event);

        var captor = org.mockito.ArgumentCaptor.forClass(ArtifactCatalogDocument.class);
        verify(artifactCatalogRepository).save(captor.capture());
        ArtifactCatalogDocument saved = captor.getValue();
        assertThat(saved.getCurrentVersion()).isEqualTo(2);
        assertThat(saved.getVersions()).hasSize(1);
        assertThat(saved.isDeleted()).isFalse();
    }
}
