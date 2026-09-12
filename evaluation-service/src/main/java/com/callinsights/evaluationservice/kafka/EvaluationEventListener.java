package com.callinsights.evaluationservice.kafka;

import com.callinsights.evaluationservice.model.ArtifactRegenerationRequestedEvent;
import com.callinsights.evaluationservice.model.CallTranscriptGeneratedEvent;
import com.callinsights.evaluationservice.service.EvaluationOrchestrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class EvaluationEventListener {

    private static final Logger log = LoggerFactory.getLogger(EvaluationEventListener.class);
    private static final String ARTIFACT_TYPE_EVALUATION = "evaluation";

    private final EvaluationOrchestrator orchestrator;

    public EvaluationEventListener(EvaluationOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @KafkaListener(
            topics = "${evaluation-service.topic.call-transcript-generated}",
            groupId = "evaluation-service-transcript-generated",
            containerFactory = "transcriptGeneratedListenerFactory")
    public void onTranscriptGenerated(CallTranscriptGeneratedEvent event) {
        log.info("Received call-transcript-generated callId={}", event.callId());
        orchestrator.handleTranscriptGenerated(event);
    }

    @KafkaListener(
            topics = "${evaluation-service.topic.artifact-regeneration-requested}",
            groupId = "evaluation-service-regeneration-requested",
            containerFactory = "regenerationRequestedListenerFactory")
    public void onRegenerationRequested(ArtifactRegenerationRequestedEvent event) {
        if (!ARTIFACT_TYPE_EVALUATION.equals(event.artifactType())) {
            return;
        }
        log.info("Received evaluation regeneration request callId={} requestedBy={}",
                event.callId(), event.requestedBy());
        orchestrator.handleRegenerationRequested(event);
    }
}
