package com.callinsights.transcriptionservice.kafka;

import com.callinsights.transcriptionservice.model.ArtifactRegenerationRequestedEvent;
import com.callinsights.transcriptionservice.model.CallCompletedEvent;
import com.callinsights.transcriptionservice.service.TranscriptionOrchestrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class TranscriptionEventListener {

    private static final Logger log = LoggerFactory.getLogger(TranscriptionEventListener.class);
    private static final String ARTIFACT_TYPE_TRANSCRIPT = "transcript";

    private final TranscriptionOrchestrator orchestrator;

    public TranscriptionEventListener(TranscriptionOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @KafkaListener(
            topics = "${transcription-service.topic.call-completed}",
            groupId = "transcription-service-call-completed",
            containerFactory = "callCompletedListenerFactory")
    public void onCallCompleted(CallCompletedEvent event) {
        log.info("Received call-completed callId={}", event.callId());
        orchestrator.handleCallCompleted(event);
    }

    @KafkaListener(
            topics = "${transcription-service.topic.artifact-regeneration-requested}",
            groupId = "transcription-service-regeneration-requested",
            containerFactory = "regenerationRequestedListenerFactory")
    public void onRegenerationRequested(ArtifactRegenerationRequestedEvent event) {
        if (!ARTIFACT_TYPE_TRANSCRIPT.equals(event.artifactType())) {
            return;
        }
        log.info("Received transcript regeneration request callId={} requestedBy={}",
                event.callId(), event.requestedBy());
        orchestrator.handleRegenerationRequested(event);
    }
}
