package com.callinsights.summaryservice.service.provider;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.errors.AnthropicIoException;
import com.anthropic.errors.AnthropicServiceException;
import com.anthropic.errors.RateLimitException;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.OutputConfig;
import com.callinsights.summaryservice.config.SummaryServiceProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AnthropicSummaryProvider implements SummaryProvider {

    private static final Logger log = LoggerFactory.getLogger(AnthropicSummaryProvider.class);

    private static final String SYSTEM_PROMPT = """
            You write concise, professional call-summary reports for a call-center supervisor
            dashboard. Given a call transcript, produce a 2-3 sentence summary of what happened,
            followed by 3-5 bullet key insights (customer sentiment, unresolved issues, notable
            moments, next steps). Output plain text only - no markdown headers.
            """;

    private final AnthropicClient client = AnthropicOkHttpClient.fromEnv();
    private final SummaryServiceProperties properties;

    public AnthropicSummaryProvider(SummaryServiceProperties properties) {
        this.properties = properties;
    }

    @Override
    public String summarize(String callId, String agentId, String templateId, String transcriptText, String model) {
        String userPrompt = """
                Call ID: %s
                Agent ID: %s
                Template: %s

                Transcript:
                %s

                Summarize this call now.
                """.formatted(callId, agentId, templateId, transcriptText);

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
            throw new SummaryProviderException("Rate limited by Anthropic API for callId " + callId, e);
        } catch (AnthropicServiceException e) {
            throw new SummaryProviderException("Anthropic API error for callId " + callId, e);
        } catch (AnthropicIoException e) {
            throw new SummaryProviderException("Network error calling Anthropic API for callId " + callId, e);
        }
    }
}
