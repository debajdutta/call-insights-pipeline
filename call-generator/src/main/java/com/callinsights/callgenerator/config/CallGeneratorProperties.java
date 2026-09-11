package com.callinsights.callgenerator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "call-generator")
public class CallGeneratorProperties {

    private Media media = new Media();
    private Topic topic = new Topic();
    private List<String> agentPool;
    private List<String> templatePool;

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

    public List<String> getAgentPool() {
        return agentPool;
    }

    public void setAgentPool(List<String> agentPool) {
        this.agentPool = agentPool;
    }

    public List<String> getTemplatePool() {
        return templatePool;
    }

    public void setTemplatePool(List<String> templatePool) {
        this.templatePool = templatePool;
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

        public String getCallCompleted() {
            return callCompleted;
        }

        public void setCallCompleted(String callCompleted) {
            this.callCompleted = callCompleted;
        }
    }
}
