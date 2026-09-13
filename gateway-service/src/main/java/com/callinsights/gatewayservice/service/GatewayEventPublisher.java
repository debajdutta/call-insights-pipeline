package com.callinsights.gatewayservice.service;

import com.callinsights.gatewayservice.model.ArtifactDeletedEvent;
import com.callinsights.gatewayservice.model.ArtifactRegenerationRequestedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class GatewayEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(GatewayEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String regenerationRequestedTopic;
    private final String artifactDeletedTopic;

    public GatewayEventPublisher(KafkaTemplate<String, Object> kafkaTemplate,
                                  @Value("${gateway-service.topic.artifact-regeneration-requested}") String regenerationRequestedTopic,
                                  @Value("${gateway-service.topic.artifact-deleted}") String artifactDeletedTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.regenerationRequestedTopic = regenerationRequestedTopic;
        this.artifactDeletedTopic = artifactDeletedTopic;
    }

    public void publishRegenerationRequested(ArtifactRegenerationRequestedEvent event) {
        log.info("Publishing artifact-regeneration-requested callId={} artifactType={} requestedBy={}",
                event.callId(), event.artifactType(), event.requestedBy());
        kafkaTemplate.send(regenerationRequestedTopic, event.callId(), event);
    }

    public void publishArtifactDeleted(ArtifactDeletedEvent event) {
        log.info("Publishing artifact-deleted callId={} artifactType={} version={} deletedBy={}",
                event.callId(), event.artifactType(), event.version(), event.deletedBy());
        kafkaTemplate.send(artifactDeletedTopic, event.callId(), event);
    }
}
