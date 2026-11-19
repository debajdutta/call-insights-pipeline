package com.callinsights.summaryservice.service;

import com.callinsights.summaryservice.config.SummaryServiceProperties;
import com.callinsights.summaryservice.model.ArtifactRegenerationRequestedEvent;
import com.callinsights.summaryservice.model.CallSummaryGeneratedEvent;
import com.callinsights.summaryservice.model.CallTranscriptGeneratedEvent;
import com.callinsights.summaryservice.model.SummaryArtifact;
import com.callinsights.summaryservice.model.TranscriptArtifact;
import com.callinsights.summaryservice.service.provider.SummaryProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for SummaryOrchestrator with a stub SummaryProvider: no LLM, no Kafka broker,
 * no Spring context. Artifacts are written to a JUnit temp directory.
 */
class SummaryOrchestratorTest {

    private static final String SUMMARY_TOPIC = "call-summary-generated";
    private static final String DEFAULT_MODEL = "default-model";

    @TempDir
    Path mediaRoot;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final List<ProviderCall> providerCalls = new ArrayList<>();
    private final SummaryProvider stubProvider = (callId, agentId, templateId, transcriptText, model) -> {
        providerCalls.add(new ProviderCall(callId, agentId, templateId, transcriptText, model));
        return "Stub summary for " + callId;
    };

    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);

    private SummaryOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        SummaryServiceProperties properties = new SummaryServiceProperties();
        properties.getMedia().setBaseDirectory(mediaRoot.toString());
        properties.getTopic().setCallSummaryGenerated(SUMMARY_TOPIC);
        properties.setModel(DEFAULT_MODEL);
        orchestrator = new SummaryOrchestrator(
                stubProvider, new ArtifactVersionResolver(), kafkaTemplate, properties, objectMapper);
    }

    @Test
    void transcriptGenerated_writesFirstSummaryAndPublishesEvent() throws IOException {
        Path transcriptFile = writeTranscript("call-1", 1, "agent-001", "TEMPLATE_SALES_CALL");

        orchestrator.handleTranscriptGenerated(new CallTranscriptGeneratedEvent(
                "call-1", "agent-001", "TEMPLATE_SALES_CALL", transcriptFile.toString(), 1, "transcript-model"));

        assertThat(providerCalls).containsExactly(new ProviderCall(
                "call-1", "agent-001", "TEMPLATE_SALES_CALL", "Agent: hello. Customer: hi.", DEFAULT_MODEL));

        Path summaryFile = mediaRoot.resolve("call-1").resolve("summary_v1.json");
        SummaryArtifact artifact = objectMapper.readValue(summaryFile.toFile(), SummaryArtifact.class);
        assertThat(artifact.callId()).isEqualTo("call-1");
        assertThat(artifact.version()).isEqualTo(1);
        assertThat(artifact.modelUsed()).isEqualTo(DEFAULT_MODEL);
        assertThat(artifact.summaryText()).isEqualTo("Stub summary for call-1");

        ArgumentCaptor<Object> sent = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(eq(SUMMARY_TOPIC), eq("call-1"), sent.capture());
        assertThat(sent.getValue()).isEqualTo(new CallSummaryGeneratedEvent(
                "call-1", "agent-001", "TEMPLATE_SALES_CALL", summaryFile.toString(), 1, DEFAULT_MODEL));
    }

    @Test
    void regenerationRequested_usesRequestedModelAndFallsBackToTranscriptContext() throws IOException {
        writeTranscript("call-2", 1, "agent-002", "TEMPLATE_SUPPORT_CALL");
        Files.writeString(mediaRoot.resolve("call-2").resolve("summary_v1.json"), "{}");

        orchestrator.handleRegenerationRequested(new ArtifactRegenerationRequestedEvent(
                "call-2", "summary", "requested-model", "supervisor", null, null));

        assertThat(providerCalls).containsExactly(new ProviderCall(
                "call-2", "agent-002", "TEMPLATE_SUPPORT_CALL", "Agent: hello. Customer: hi.", "requested-model"));

        Path summaryFile = mediaRoot.resolve("call-2").resolve("summary_v2.json");
        assertThat(summaryFile).exists();

        ArgumentCaptor<Object> sent = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(eq(SUMMARY_TOPIC), eq("call-2"), sent.capture());
        assertThat(sent.getValue()).isEqualTo(new CallSummaryGeneratedEvent(
                "call-2", "agent-002", "TEMPLATE_SUPPORT_CALL", summaryFile.toString(), 2, "requested-model"));
    }

    @Test
    void regenerationRequested_withoutTranscriptFails() {
        assertThatThrownBy(() -> orchestrator.handleRegenerationRequested(new ArtifactRegenerationRequestedEvent(
                "call-3", "summary", null, "supervisor", null, null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("call-3");

        assertThat(providerCalls).isEmpty();
        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
    }

    private Path writeTranscript(String callId, int version, String agentId, String templateId) throws IOException {
        Path callDirectory = Files.createDirectories(mediaRoot.resolve(callId));
        Path file = callDirectory.resolve("transcript_v" + version + ".json");
        objectMapper.writeValue(file.toFile(), new TranscriptArtifact(
                callId, agentId, templateId, version, "transcript-model", "2026-01-01T00:00:00Z",
                "Agent: hello. Customer: hi."));
        return file;
    }

    private record ProviderCall(String callId, String agentId, String templateId, String transcriptText,
                                String model) {
    }
}
