package com.callinsights.catalogservice.config;

import com.callinsights.catalogservice.model.ArtifactDeletedEvent;
import com.callinsights.catalogservice.model.CallCompletedEvent;
import com.callinsights.catalogservice.model.CallEvaluationGeneratedEvent;
import com.callinsights.catalogservice.model.CallSummaryGeneratedEvent;
import com.callinsights.catalogservice.model.CallTranscriptGeneratedEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ConsumerFactory<String, CallCompletedEvent> callCompletedConsumerFactory() {
        return consumerFactory("catalog-service-call-completed", CallCompletedEvent.class);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, CallCompletedEvent> callCompletedListenerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, CallCompletedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(callCompletedConsumerFactory());
        return factory;
    }

    @Bean
    public ConsumerFactory<String, CallTranscriptGeneratedEvent> transcriptGeneratedConsumerFactory() {
        return consumerFactory("catalog-service-transcript-generated", CallTranscriptGeneratedEvent.class);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, CallTranscriptGeneratedEvent> transcriptGeneratedListenerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, CallTranscriptGeneratedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(transcriptGeneratedConsumerFactory());
        return factory;
    }

    @Bean
    public ConsumerFactory<String, CallSummaryGeneratedEvent> summaryGeneratedConsumerFactory() {
        return consumerFactory("catalog-service-summary-generated", CallSummaryGeneratedEvent.class);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, CallSummaryGeneratedEvent> summaryGeneratedListenerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, CallSummaryGeneratedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(summaryGeneratedConsumerFactory());
        return factory;
    }

    @Bean
    public ConsumerFactory<String, CallEvaluationGeneratedEvent> evaluationGeneratedConsumerFactory() {
        return consumerFactory("catalog-service-evaluation-generated", CallEvaluationGeneratedEvent.class);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, CallEvaluationGeneratedEvent> evaluationGeneratedListenerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, CallEvaluationGeneratedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(evaluationGeneratedConsumerFactory());
        return factory;
    }

    @Bean
    public ConsumerFactory<String, ArtifactDeletedEvent> artifactDeletedConsumerFactory() {
        return consumerFactory("catalog-service-artifact-deleted", ArtifactDeletedEvent.class);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ArtifactDeletedEvent> artifactDeletedListenerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, ArtifactDeletedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(artifactDeletedConsumerFactory());
        return factory;
    }

    private <T> ConsumerFactory<String, T> consumerFactory(String groupId, Class<T> targetType) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        JsonDeserializer<T> valueDeserializer = new JsonDeserializer<>(targetType, false);
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), valueDeserializer);
    }
}
