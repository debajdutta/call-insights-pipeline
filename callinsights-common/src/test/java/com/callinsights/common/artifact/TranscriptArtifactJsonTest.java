package com.callinsights.common.artifact;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the transcript_v{n}.json file format. transcription-service writes it, summary-service and
 * evaluation-service read it, so a component rename, removal or addition fails here.
 */
class TranscriptArtifactJsonTest {

    private static final List<String> FIELDS = List.of(
            "callId", "agentId", "templateId", "version", "modelUsed", "generatedAt", "transcriptText");

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void serialisesExactlyTheSevenFieldNames() throws Exception {
        TranscriptArtifact artifact = new TranscriptArtifact("call-synthetic-0001", "agent-001",
                "TEMPLATE_SALES_CALL", 1, "model-a", "2026-01-01T00:00:00Z", "Agent: Hello.");

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(artifact));

        List<String> names = new ArrayList<>();
        json.fieldNames().forEachRemaining(names::add);
        assertThat(names).containsExactlyElementsOf(FIELDS);
        assertThat(json.get("version").isInt()).isTrue();
        assertThat(json.get("callId").asText()).isEqualTo("call-synthetic-0001");
    }

    @Test
    void deserialisesTodaysFileFormat() throws Exception {
        TranscriptArtifact artifact;
        try (InputStream in = getClass().getResourceAsStream("/artifact/transcript_v1.json")) {
            assertThat(in).isNotNull();
            artifact = objectMapper.readValue(in, TranscriptArtifact.class);
        }

        assertThat(artifact).isEqualTo(new TranscriptArtifact("call-synthetic-0001", "agent-001",
                "TEMPLATE_SALES_CALL", 1, "model-a", "2026-01-01T00:00:00Z",
                "Agent: Hello, thanks for calling.\nCustomer: Hi, I have a question about my plan."));
    }
}
