package com.callinsights.summaryservice.config;

import com.callinsights.common.artifact.ArtifactVersionResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the shared, Spring-free ArtifactVersionResolver from callinsights-common as a bean.
 */
@Configuration
public class ArtifactConfig {

    @Bean
    public ArtifactVersionResolver artifactVersionResolver() {
        return new ArtifactVersionResolver();
    }
}
