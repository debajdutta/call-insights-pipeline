package com.callinsights.summaryservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "summary-service")
public class SummaryServiceProperties {

    private Media media = new Media();
    private Topic topic = new Topic();
    private String model;
    private int maxTokens;

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
        private String callTranscriptGenerated;
        private String artifactRegenerationRequested;
        private String callSummaryGenerated;

        public String getCallTranscriptGenerated() {
            return callTranscriptGenerated;
        }

        public void setCallTranscriptGenerated(String callTranscriptGenerated) {
            this.callTranscriptGenerated = callTranscriptGenerated;
        }

        public String getArtifactRegenerationRequested() {
            return artifactRegenerationRequested;
        }

        public void setArtifactRegenerationRequested(String artifactRegenerationRequested) {
            this.artifactRegenerationRequested = artifactRegenerationRequested;
        }

        public String getCallSummaryGenerated() {
            return callSummaryGenerated;
        }

        public void setCallSummaryGenerated(String callSummaryGenerated) {
            this.callSummaryGenerated = callSummaryGenerated;
        }
    }
}
