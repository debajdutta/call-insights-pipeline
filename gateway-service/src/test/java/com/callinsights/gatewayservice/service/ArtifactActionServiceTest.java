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
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArtifactActionServiceTest {

    private final CallRepository callRepository = mock(CallRepository.class);
    private final ArtifactCatalogRepository artifactCatalogRepository = mock(ArtifactCatalogRepository.class);
    private final GatewayEventPublisher eventPublisher = mock(GatewayEventPublisher.class);
    private final ArtifactActionService service =
            new ArtifactActionService(callRepository, artifactCatalogRepository, eventPublisher);

    @Test
    void regenerate_publishesEventWithCallContext() {
        CallDocument call = new CallDocument();
        call.setCallId("call-1");
        call.setAgentId("agent-001");
        call.setTemplateId("TEMPLATE_SALES_CALL");
        when(callRepository.findById("call-1")).thenReturn(Optional.of(call));

        service.regenerate("call-1", "transcript", "claude-opus-5", "supervisor");

        var captor = org.mockito.ArgumentCaptor.forClass(ArtifactRegenerationRequestedEvent.class);
        verify(eventPublisher).publishRegenerationRequested(captor.capture());
        ArtifactRegenerationRequestedEvent event = captor.getValue();
        assertThat(event.callId()).isEqualTo("call-1");
        assertThat(event.artifactType()).isEqualTo("transcript");
        assertThat(event.requestedModel()).isEqualTo("claude-opus-5");
        assertThat(event.requestedBy()).isEqualTo("supervisor");
        assertThat(event.agentId()).isEqualTo("agent-001");
        assertThat(event.templateId()).isEqualTo("TEMPLATE_SALES_CALL");
    }

    @Test
    void regenerate_rejectsUnknownArtifactType() {
        assertThatThrownBy(() -> service.regenerate("call-1", "video", "model", "supervisor"))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void regenerate_rejectsUnknownCall() {
        when(callRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.regenerate("missing", "transcript", "model", "supervisor"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void delete_publishesEventWithCurrentVersion() {
        ArtifactCatalogDocument catalogEntry = new ArtifactCatalogDocument();
        catalogEntry.setCallId("call-1");
        catalogEntry.setArtifactType("summary");
        catalogEntry.setCurrentVersion(2);
        ArtifactVersionEntry v1 = new ArtifactVersionEntry();
        v1.setVersion(1);
        v1.setPath("/tmp/does-not-exist-v1.json");
        ArtifactVersionEntry v2 = new ArtifactVersionEntry();
        v2.setVersion(2);
        v2.setPath("/tmp/does-not-exist-v2.json");
        catalogEntry.setVersions(List.of(v1, v2));
        when(artifactCatalogRepository.findById("call-1:summary")).thenReturn(Optional.of(catalogEntry));

        int deletedVersion = service.delete("call-1", "summary", "supervisor");

        assertThat(deletedVersion).isEqualTo(2);
        var captor = org.mockito.ArgumentCaptor.forClass(ArtifactDeletedEvent.class);
        verify(eventPublisher).publishArtifactDeleted(captor.capture());
        assertThat(captor.getValue().version()).isEqualTo(2);
        assertThat(captor.getValue().deletedBy()).isEqualTo("supervisor");
    }

    @Test
    void delete_rejectsAlreadyDeletedArtifact() {
        ArtifactCatalogDocument catalogEntry = new ArtifactCatalogDocument();
        catalogEntry.setDeleted(true);
        catalogEntry.setCurrentVersion(1);
        when(artifactCatalogRepository.findById("call-1:transcript")).thenReturn(Optional.of(catalogEntry));

        assertThatThrownBy(() -> service.delete("call-1", "transcript", "supervisor"))
                .isInstanceOf(NotFoundException.class);
        verify(eventPublisher, never()).publishArtifactDeleted(any());
    }
}
