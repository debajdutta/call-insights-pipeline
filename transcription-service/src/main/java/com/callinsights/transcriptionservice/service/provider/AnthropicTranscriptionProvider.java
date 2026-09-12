package com.callinsights.transcriptionservice.service.provider;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.errors.AnthropicIoException;
import com.anthropic.errors.AnthropicServiceException;
import com.anthropic.errors.RateLimitException;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.OutputConfig;
import com.callinsights.transcriptionservice.config.TranscriptionServiceProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Uses an LLM as a mock transcriber rather than real speech-to-text: the dummy media from
 * call-generator is a silent WAV, so real ASR would honestly return an empty transcript.
 * Generating a plausible synthetic transcript from the call's template/agent context is more
 * useful for this demo and still exercises a real, live call to a real model provider
 * (SPEC.md §6 explicitly allows "an LLM-based mock transcription").
 */
@Component
public class AnthropicTranscriptionProvider implements TranscriptionProvider {

    private static final Logger log = LoggerFactory.getLogger(AnthropicTranscriptionProvider.class);

    private static final String SYSTEM_PROMPT = """
            You generate entirely synthetic, fictional call-center transcripts for a demo system.
            No real customer data is involved. Given a call template category, invent a short,
            plausible transcript (6-10 turns) between an "Agent" and a "Customer" that fits that
            category. Output plain text only, formatted as alternating "Agent:" / "Customer:" lines.
            """;

    private final AnthropicClient client = AnthropicOkHttpClient.fromEnv();
    private final TranscriptionServiceProperties properties;

    public AnthropicTranscriptionProvider(TranscriptionServiceProperties properties) {
        this.properties = properties;
    }

    @Override
    public String transcribe(String callId, String agentId, String templateId, String model) {
        String scenario = properties.getScenarios() != null ? properties.getScenarios().get(templateId) : null;
        String userPrompt = buildUserPrompt(callId, agentId, templateId, scenario);

        MessageCreateParams params = MessageCreateParams.builder()
                .model(model)
                .maxTokens(properties.getMaxTokens())
                .system(SYSTEM_PROMPT)
                .outputConfig(OutputConfig.builder().effort(OutputConfig.Effort.LOW).build())
                .addUserMessage(userPrompt)
                .build();

        try {
            Message response = client.messages().create(params);
            log.info("Anthropic usage callId={} model={} inputTokens={} outputTokens={}",
                    callId, model, response.usage().inputTokens(), response.usage().outputTokens());
            return response.content().stream()
                    .flatMap(block -> block.text().stream())
                    .map(textBlock -> textBlock.text())
                    .reduce("", String::concat);
        } catch (RateLimitException e) {
            throw new TranscriptionProviderException("Rate limited by Anthropic API for callId " + callId, e);
        } catch (AnthropicServiceException e) {
            throw new TranscriptionProviderException("Anthropic API error for callId " + callId, e);
        } catch (AnthropicIoException e) {
            throw new TranscriptionProviderException("Network error calling Anthropic API for callId " + callId, e);
        }
    }

    /**
     * When a known scenario is configured for this templateId, anchor the transcript to it so
     * outputs are verifiable against a known ground truth instead of freely improvised - a
     * lighter-weight stand-in for real audio + real ASR (no new infrastructure, just a prompt
     * change). Falls back to the original freeform behavior when no scenario is configured for
     * that template.
     */
    private String buildUserPrompt(String callId, String agentId, String templateId, String scenario) {
        if (scenario == null) {
            return "Call ID: %s\nAgent ID: %s\nTemplate: %s\nGenerate the transcript now."
                    .formatted(callId, agentId, templateId);
        }
        return """
                Call ID: %s
                Agent ID: %s
                Template: %s

                Scenario (base the transcript on this specific situation, playing it out
                naturally in dialogue - do not just restate it): %s

                Generate the transcript now.
                """.formatted(callId, agentId, templateId, scenario);
    }
}
