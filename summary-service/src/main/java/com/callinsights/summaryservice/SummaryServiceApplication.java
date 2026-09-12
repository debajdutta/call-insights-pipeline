package com.callinsights.summaryservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SummaryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SummaryServiceApplication.class, args);
    }
}
