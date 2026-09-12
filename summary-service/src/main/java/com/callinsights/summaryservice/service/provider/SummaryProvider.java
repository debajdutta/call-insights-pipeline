package com.callinsights.summaryservice.service.provider;

public interface SummaryProvider {

    String summarize(String callId, String agentId, String templateId, String transcriptText, String model);
}
