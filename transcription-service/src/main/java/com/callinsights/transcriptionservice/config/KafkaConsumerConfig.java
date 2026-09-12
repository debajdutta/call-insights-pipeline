package com.callinsights.transcriptionservice.config;

import com.callinsights.transcriptionservice.model.ArtifactRegenerationRequestedEvent;
import com.callinsights.transcriptionservice.model.CallCompletedEvent;
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

/**
 * Two explicit ConsumerFactory/ContainerFactory pairs, one per topic, each pinned to its own
 * concrete event class. Deliberately not relying on Spring Kafka's "inferred type" JSON
 * deserialization (method-parameter-based type inference) - that mechanism is easy to
 * misconfigure silently; explicit factories are simpler to reason about and verify.
 */
@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ConsumerFactory<String, CallCompletedEvent> callCompletedConsumerFactory() {
        return consumerFactory("transcription-service-call-completed", CallCompletedEvent.class);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, CallCompletedEvent> callCompletedListenerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, CallCompletedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(callCompletedConsumerFactory());
        return factory;
    }

    @Bean
    public ConsumerFactory<String, ArtifactRegenerationRequestedEvent> regenerationRequestedConsumerFactory() {
        return consumerFactory("transcription-service-regeneration-requested", ArtifactRegenerationRequestedEvent.class);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ArtifactRegenerationRequestedEvent> regenerationRequestedListenerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, ArtifactRegenerationRequestedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(regenerationRequestedConsumerFactory());
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
