package com.callinsights.callgenerator.service;

import com.callinsights.callgenerator.config.CallGeneratorProperties;
import com.callinsights.callgenerator.model.CallCompletedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class CallGeneratorService {

    private static final Logger log = LoggerFactory.getLogger(CallGeneratorService.class);

    private final MediaFileGenerator mediaFileGenerator;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final CallGeneratorProperties properties;

    public CallGeneratorService(MediaFileGenerator mediaFileGenerator,
                                 KafkaTemplate<String, Object> kafkaTemplate,
                                 CallGeneratorProperties properties) {
        this.mediaFileGenerator = mediaFileGenerator;
        this.kafkaTemplate = kafkaTemplate;
        this.properties = properties;
    }

    public CallCompletedEvent generateCall() {
        String callId = UUID.randomUUID().toString();
        String agentId = pickRandom(properties.getAgentPool());
        String templateId = pickRandom(properties.getTemplatePool());
        String timestamp = Instant.now().toString();

        Path callDirectory = Path.of(properties.getMedia().getBaseDirectory(), callId);
        Path mediaFile;
        try {
            mediaFile = mediaFileGenerator.generate(callDirectory, "call.wav");
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to generate media file for call " + callId, e);
        }

        CallCompletedEvent event = new CallCompletedEvent(
                callId, agentId, templateId, mediaFile.toString(), timestamp);

        kafkaTemplate.send(properties.getTopic().getCallCompleted(), callId, event);

        log.info("Generated call callId={} agentId={} templateId={} mediaPath={}",
                callId, agentId, templateId, mediaFile);

        return event;
    }

    private String pickRandom(List<String> pool) {
        return pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
    }
}
