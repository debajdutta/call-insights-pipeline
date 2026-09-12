package com.callinsights.evaluationservice.service;

import com.callinsights.evaluationservice.config.EvaluationServiceProperties;
import com.callinsights.evaluationservice.model.ArtifactRegenerationRequestedEvent;
import com.callinsights.evaluationservice.model.CallEvaluationGeneratedEvent;
import com.callinsights.evaluationservice.model.CallTranscriptGeneratedEvent;
import com.callinsights.evaluationservice.model.EvaluationArtifact;
import com.callinsights.evaluationservice.model.TranscriptArtifact;
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
import java.util.List;

@Service
public class EvaluationOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(EvaluationOrchestrator.class);
    private static final String TRANSCRIPT_ARTIFACT_BASE_NAME = "transcript";
    private static final String EVALUATION_ARTIFACT_BASE_NAME = "evaluation";

    private final KeywordScorer keywordScorer;
    private final ArtifactVersionResolver versionResolver;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final EvaluationServiceProperties properties;
    private final ObjectMapper objectMapper;

    public EvaluationOrchestrator(KeywordScorer keywordScorer,
                                   ArtifactVersionResolver versionResolver,
                                   KafkaTemplate<String, Object> kafkaTemplate,
                                   EvaluationServiceProperties properties,
                                   ObjectMapper objectMapper) {
        this.keywordScorer = keywordScorer;
        this.versionResolver = versionResolver;
        this.kafkaTemplate = kafkaTemplate;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public void handleTranscriptGenerated(CallTranscriptGeneratedEvent event) {
        Path transcriptFile = Path.of(event.transcriptPath());
        TranscriptArtifact transcript = readTranscript(transcriptFile);
        Path callDirectory = transcriptFile.getParent();
        generate(event.callId(), event.agentId(), event.templateId(), transcript.transcriptText(), callDirectory);
    }

    public void handleRegenerationRequested(ArtifactRegenerationRequestedEvent event) {
        Path callDirectory = Path.of(properties.getMedia().getBaseDirectory(), event.callId());
        Path transcriptFile = versionResolver.latestArtifactPath(callDirectory, TRANSCRIPT_ARTIFACT_BASE_NAME)
                .orElseThrow(() -> new IllegalStateException(
                        "No transcript found for callId " + event.callId() + " - cannot regenerate evaluation"));
        TranscriptArtifact transcript = readTranscript(transcriptFile);

        String agentId = event.agentId() != null ? event.agentId() : transcript.agentId();
        String templateId = event.templateId() != null ? event.templateId() : transcript.templateId();
        generate(event.callId(), agentId, templateId, transcript.transcriptText(), callDirectory);
    }

    private void generate(String callId, String agentId, String templateId, String transcriptText, Path callDirectory) {
        List<String> keywords = properties.getScoring().getRules().getOrDefault(templateId, List.of());
        KeywordScorer.ScoringResult result =
                keywordScorer.score(transcriptText, keywords, properties.getScoring().getPassThreshold());

        int version = versionResolver.nextVersion(callDirectory, EVALUATION_ARTIFACT_BASE_NAME);
        String rulesetVersion = properties.getScoring().getRulesetVersion();

        EvaluationArtifact artifact = new EvaluationArtifact(
                callId, agentId, templateId, version, rulesetVersion, Instant.now().toString(),
                result.score(), result.passed() ? "PASS" : "FAIL",
                result.matchedKeywords(), result.missingKeywords());

        Path evaluationFile = callDirectory.resolve(EVALUATION_ARTIFACT_BASE_NAME + "_v" + version + ".json");
        writeArtifact(evaluationFile, artifact);

        CallEvaluationGeneratedEvent generatedEvent = new CallEvaluationGeneratedEvent(
                callId, agentId, templateId, evaluationFile.toString(), version, rulesetVersion);
        kafkaTemplate.send(properties.getTopic().getCallEvaluationGenerated(), callId, generatedEvent);

        log.info("Generated evaluation callId={} version={} score={} verdict={} path={}",
                callId, version, result.score(), result.passed() ? "PASS" : "FAIL", evaluationFile);
    }

    private TranscriptArtifact readTranscript(Path transcriptFile) {
        try {
            return objectMapper.readValue(transcriptFile.toFile(), TranscriptArtifact.class);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read transcript artifact " + transcriptFile, e);
        }
    }

    private void writeArtifact(Path file, EvaluationArtifact artifact) {
        try {
            Files.createDirectories(file.getParent());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), artifact);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write evaluation artifact " + file, e);
        }
    }
}
