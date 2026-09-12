package com.callinsights.summaryservice.service;

import com.callinsights.summaryservice.config.SummaryServiceProperties;
import com.callinsights.summaryservice.model.ArtifactRegenerationRequestedEvent;
import com.callinsights.summaryservice.model.CallSummaryGeneratedEvent;
import com.callinsights.summaryservice.model.CallTranscriptGeneratedEvent;
import com.callinsights.summaryservice.model.SummaryArtifact;
import com.callinsights.summaryservice.model.TranscriptArtifact;
import com.callinsights.summaryservice.service.provider.SummaryProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

@Service
public class SummaryOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(SummaryOrchestrator.class);
    private static final String TRANSCRIPT_ARTIFACT_BASE_NAME = "transcript";
    private static final String SUMMARY_ARTIFACT_BASE_NAME = "summary";

    private final SummaryProvider provider;
    private final ArtifactVersionResolver versionResolver;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final SummaryServiceProperties properties;
    private final ObjectMapper objectMapper;

    public SummaryOrchestrator(SummaryProvider provider,
                                ArtifactVersionResolver versionResolver,
                                KafkaTemplate<String, Object> kafkaTemplate,
                                SummaryServiceProperties properties,
                                ObjectMapper objectMapper) {
        this.provider = provider;
        this.versionResolver = versionResolver;
        this.kafkaTemplate = kafkaTemplate;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public void handleTranscriptGenerated(CallTranscriptGeneratedEvent event) {
        Path transcriptFile = Path.of(event.transcriptPath());
        TranscriptArtifact transcript = readTranscript(transcriptFile);
        Path callDirectory = transcriptFile.getParent();
        generate(event.callId(), event.agentId(), event.templateId(), transcript.transcriptText(),
                callDirectory, properties.getModel());
    }

    public void handleRegenerationRequested(ArtifactRegenerationRequestedEvent event) {
        Path callDirectory = Path.of(properties.getMedia().getBaseDirectory(), event.callId());
        Path transcriptFile = versionResolver.latestArtifactPath(callDirectory, TRANSCRIPT_ARTIFACT_BASE_NAME)
                .orElseThrow(() -> new IllegalStateException(
                        "No transcript found for callId " + event.callId() + " - cannot regenerate summary"));
        TranscriptArtifact transcript = readTranscript(transcriptFile);

        String model = event.requestedModel() != null ? event.requestedModel() : properties.getModel();
        String agentId = event.agentId() != null ? event.agentId() : transcript.agentId();
        String templateId = event.templateId() != null ? event.templateId() : transcript.templateId();
        generate(event.callId(), agentId, templateId, transcript.transcriptText(), callDirectory, model);
    }

    private void generate(String callId, String agentId, String templateId, String transcriptText,
                           Path callDirectory, String model) {
        int version = versionResolver.nextVersion(callDirectory, SUMMARY_ARTIFACT_BASE_NAME);
        String summaryText = provider.summarize(callId, agentId, templateId, transcriptText, model);

        SummaryArtifact artifact = new SummaryArtifact(
                callId, agentId, templateId, version, model, Instant.now().toString(), summaryText);

        Path summaryFile = callDirectory.resolve(SUMMARY_ARTIFACT_BASE_NAME + "_v" + version + ".json");
        writeArtifact(summaryFile, artifact);

        CallSummaryGeneratedEvent generatedEvent = new CallSummaryGeneratedEvent(
                callId, agentId, templateId, summaryFile.toString(), version, model);
        kafkaTemplate.send(properties.getTopic().getCallSummaryGenerated(), callId, generatedEvent);

        log.info("Generated summary callId={} version={} model={} path={}",
                callId, version, model, summaryFile);
    }

    private TranscriptArtifact readTranscript(Path transcriptFile) {
        try {
            return objectMapper.readValue(transcriptFile.toFile(), TranscriptArtifact.class);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read transcript artifact " + transcriptFile, e);
        }
    }

    private void writeArtifact(Path file, SummaryArtifact artifact) {
        try {
            Files.createDirectories(file.getParent());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), artifact);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write summary artifact " + file, e);
        }
    }
}
