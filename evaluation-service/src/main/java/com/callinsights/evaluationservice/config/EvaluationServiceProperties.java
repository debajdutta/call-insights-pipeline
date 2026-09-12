package com.callinsights.evaluationservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "evaluation-service")
public class EvaluationServiceProperties {

    private Media media = new Media();
    private Topic topic = new Topic();
    private Scoring scoring = new Scoring();

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

    public Scoring getScoring() {
        return scoring;
    }

    public void setScoring(Scoring scoring) {
        this.scoring = scoring;
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
        private String callEvaluationGenerated;

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

        public String getCallEvaluationGenerated() {
            return callEvaluationGenerated;
        }

        public void setCallEvaluationGenerated(String callEvaluationGenerated) {
            this.callEvaluationGenerated = callEvaluationGenerated;
        }
    }

    public static class Scoring {
        private String rulesetVersion;
        private double passThreshold;
        private Map<String, List<String>> rules;

        public String getRulesetVersion() {
            return rulesetVersion;
        }

        public void setRulesetVersion(String rulesetVersion) {
            this.rulesetVersion = rulesetVersion;
        }

        public double getPassThreshold() {
            return passThreshold;
        }

        public void setPassThreshold(double passThreshold) {
            this.passThreshold = passThreshold;
        }

        public Map<String, List<String>> getRules() {
            return rules;
        }

        public void setRules(Map<String, List<String>> rules) {
            this.rules = rules;
        }
    }
}
