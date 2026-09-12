package com.callinsights.transcriptionservice.service.provider;

public interface TranscriptionProvider {

    String transcribe(String callId, String agentId, String templateId, String model);
}
