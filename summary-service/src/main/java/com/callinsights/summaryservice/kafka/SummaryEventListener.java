package com.callinsights.summaryservice.kafka;

import com.callinsights.summaryservice.model.ArtifactRegenerationRequestedEvent;
import com.callinsights.summaryservice.model.CallTranscriptGeneratedEvent;
import com.callinsights.summaryservice.service.SummaryOrchestrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class SummaryEventListener {

    private static final Logger log = LoggerFactory.getLogger(SummaryEventListener.class);
    private static final String ARTIFACT_TYPE_SUMMARY = "summary";

    private final SummaryOrchestrator orchestrator;

    public SummaryEventListener(SummaryOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @KafkaListener(
            topics = "${summary-service.topic.call-transcript-generated}",
            groupId = "summary-service-transcript-generated",
            containerFactory = "transcriptGeneratedListenerFactory")
    public void onTranscriptGenerated(CallTranscriptGeneratedEvent event) {
        log.info("Received call-transcript-generated callId={}", event.callId());
        orchestrator.handleTranscriptGenerated(event);
    }

    @KafkaListener(
            topics = "${summary-service.topic.artifact-regeneration-requested}",
            groupId = "summary-service-regeneration-requested",
            containerFactory = "regenerationRequestedListenerFactory")
    public void onRegenerationRequested(ArtifactRegenerationRequestedEvent event) {
        if (!ARTIFACT_TYPE_SUMMARY.equals(event.artifactType())) {
            return;
        }
        log.info("Received summary regeneration request callId={} requestedBy={}",
                event.callId(), event.requestedBy());
        orchestrator.handleRegenerationRequested(event);
    }
}
