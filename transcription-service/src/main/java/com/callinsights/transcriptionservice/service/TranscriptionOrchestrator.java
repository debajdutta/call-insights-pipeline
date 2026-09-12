package com.callinsights.transcriptionservice.service;

import com.callinsights.transcriptionservice.config.TranscriptionServiceProperties;
import com.callinsights.transcriptionservice.model.ArtifactRegenerationRequestedEvent;
import com.callinsights.transcriptionservice.model.CallCompletedEvent;
import com.callinsights.transcriptionservice.model.CallTranscriptGeneratedEvent;
import com.callinsights.transcriptionservice.model.TranscriptArtifact;
import com.callinsights.transcriptionservice.service.provider.TranscriptionProvider;
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
public class TranscriptionOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(TranscriptionOrchestrator.class);
    private static final String ARTIFACT_BASE_NAME = "transcript";

    private final TranscriptionProvider provider;
    private final ArtifactVersionResolver versionResolver;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final TranscriptionServiceProperties properties;
    private final ObjectMapper objectMapper;

    public TranscriptionOrchestrator(TranscriptionProvider provider,
                                      ArtifactVersionResolver versionResolver,
                                      KafkaTemplate<String, Object> kafkaTemplate,
                                      TranscriptionServiceProperties properties,
                                      ObjectMapper objectMapper) {
        this.provider = provider;
        this.versionResolver = versionResolver;
        this.kafkaTemplate = kafkaTemplate;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public void handleCallCompleted(CallCompletedEvent event) {
        Path callDirectory = Path.of(event.mediaPath()).getParent();
        generate(event.callId(), event.agentId(), event.templateId(), callDirectory, properties.getModel());
    }

    public void handleRegenerationRequested(ArtifactRegenerationRequestedEvent event) {
        Path callDirectory = Path.of(properties.getMedia().getBaseDirectory(), event.callId());
        String model = event.requestedModel() != null ? event.requestedModel() : properties.getModel();
        String agentId = event.agentId() != null ? event.agentId() : "unknown-agent";
        String templateId = event.templateId() != null ? event.templateId() : "UNKNOWN_TEMPLATE";
        generate(event.callId(), agentId, templateId, callDirectory, model);
    }

    private void generate(String callId, String agentId, String templateId, Path callDirectory, String model) {
        int version = versionResolver.nextVersion(callDirectory, ARTIFACT_BASE_NAME);
        String transcriptText = provider.transcribe(callId, agentId, templateId, model);

        TranscriptArtifact artifact = new TranscriptArtifact(
                callId, agentId, templateId, version, model, Instant.now().toString(), transcriptText);

        Path transcriptFile = callDirectory.resolve(ARTIFACT_BASE_NAME + "_v" + version + ".json");
        writeArtifact(transcriptFile, artifact);

        CallTranscriptGeneratedEvent generatedEvent = new CallTranscriptGeneratedEvent(
                callId, agentId, templateId, transcriptFile.toString(), version, model);
        kafkaTemplate.send(properties.getTopic().getCallTranscriptGenerated(), callId, generatedEvent);

        log.info("Generated transcript callId={} version={} model={} path={}",
                callId, version, model, transcriptFile);
    }

    private void writeArtifact(Path file, TranscriptArtifact artifact) {
        try {
            Files.createDirectories(file.getParent());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), artifact);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write transcript artifact " + file, e);
        }
    }
}
