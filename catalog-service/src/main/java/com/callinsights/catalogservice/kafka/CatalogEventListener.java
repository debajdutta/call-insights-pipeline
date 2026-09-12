package com.callinsights.catalogservice.kafka;

import com.callinsights.catalogservice.model.ArtifactDeletedEvent;
import com.callinsights.catalogservice.model.CallCompletedEvent;
import com.callinsights.catalogservice.model.CallEvaluationGeneratedEvent;
import com.callinsights.catalogservice.model.CallSummaryGeneratedEvent;
import com.callinsights.catalogservice.model.CallTranscriptGeneratedEvent;
import com.callinsights.catalogservice.service.CatalogOrchestrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class CatalogEventListener {

    private static final Logger log = LoggerFactory.getLogger(CatalogEventListener.class);

    private final CatalogOrchestrator orchestrator;

    public CatalogEventListener(CatalogOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @KafkaListener(
            topics = "${catalog-service.topic.call-completed}",
            groupId = "catalog-service-call-completed",
            containerFactory = "callCompletedListenerFactory")
    public void onCallCompleted(CallCompletedEvent event) {
        log.info("Received call-completed callId={}", event.callId());
        orchestrator.handleCallCompleted(event);
    }

    @KafkaListener(
            topics = "${catalog-service.topic.call-transcript-generated}",
            groupId = "catalog-service-transcript-generated",
            containerFactory = "transcriptGeneratedListenerFactory")
    public void onTranscriptGenerated(CallTranscriptGeneratedEvent event) {
        log.info("Received call-transcript-generated callId={} version={}", event.callId(), event.version());
        orchestrator.handleTranscriptGenerated(event);
    }

    @KafkaListener(
            topics = "${catalog-service.topic.call-summary-generated}",
            groupId = "catalog-service-summary-generated",
            containerFactory = "summaryGeneratedListenerFactory")
    public void onSummaryGenerated(CallSummaryGeneratedEvent event) {
        log.info("Received call-summary-generated callId={} version={}", event.callId(), event.version());
        orchestrator.handleSummaryGenerated(event);
    }

    @KafkaListener(
            topics = "${catalog-service.topic.call-evaluation-generated}",
            groupId = "catalog-service-evaluation-generated",
            containerFactory = "evaluationGeneratedListenerFactory")
    public void onEvaluationGenerated(CallEvaluationGeneratedEvent event) {
        log.info("Received call-evaluation-generated callId={} version={}", event.callId(), event.version());
        orchestrator.handleEvaluationGenerated(event);
    }

    @KafkaListener(
            topics = "${catalog-service.topic.artifact-deleted}",
            groupId = "catalog-service-artifact-deleted",
            containerFactory = "artifactDeletedListenerFactory")
    public void onArtifactDeleted(ArtifactDeletedEvent event) {
        log.info("Received artifact-deleted callId={} artifactType={}", event.callId(), event.artifactType());
        orchestrator.handleArtifactDeleted(event);
    }
}
