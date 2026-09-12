package com.callinsights.transcriptionservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class TranscriptionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TranscriptionServiceApplication.class, args);
    }
}
