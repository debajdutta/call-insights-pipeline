package com.callinsights.transcriptionservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@ConfigurationProperties(prefix = "transcription-service")
public class TranscriptionServiceProperties {

    private Media media = new Media();
    private Topic topic = new Topic();
    private String model;
    private int maxTokens;
    private Map<String, String> scenarios;

    public Media getMedia() {
        return media;
    }

    public void setMedia(Media media) {
        this.media = media;
    }

    public Topic getTopic() {
        return topic;
    }

    public void setTopic(Topic topic) {
        this.topic = topic;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }

    public Map<String, String> getScenarios() {
        return scenarios;
    }

    public void setScenarios(Map<String, String> scenarios) {
        this.scenarios = scenarios;
    }

    public static class Media {
        private String baseDirectory;

        public String getBaseDirectory() {
            return baseDirectory;
        }

        public void setBaseDirectory(String baseDirectory) {
            this.baseDirectory = baseDirectory;
        }
    }

    public static class Topic {
        private String callCompleted;
        private String artifactRegenerationRequested;
        private String callTranscriptGenerated;

        public String getCallCompleted() {
            return callCompleted;
        }

        public void setCallCompleted(String callCompleted) {
            this.callCompleted = callCompleted;
        }

        public String getArtifactRegenerationRequested() {
            return artifactRegenerationRequested;
        }

        public void setArtifactRegenerationRequested(String artifactRegenerationRequested) {
            this.artifactRegenerationRequested = artifactRegenerationRequested;
        }

        public String getCallTranscriptGenerated() {
            return callTranscriptGenerated;
        }

        public void setCallTranscriptGenerated(String callTranscriptGenerated) {
            this.callTranscriptGenerated = callTranscriptGenerated;
        }
    }
}
