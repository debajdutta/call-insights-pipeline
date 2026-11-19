package com.callinsights.common.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the JSON wire format of every Kafka event. The golden JSON strings below are the
 * payload shapes the services exchanged before the records moved into this module; a field
 * rename, removal or addition fails here.
 */
class EventJsonContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void callCompleted() throws Exception {
        assertContract(
                new CallCompletedEvent("call-1", "agent-001", "TEMPLATE_SALES_CALL",
                        "/media/call-1/call.wav", "2026-01-01T00:00:00Z"),
                """
                {"callId":"call-1","agentId":"agent-001","templateId":"TEMPLATE_SALES_CALL",
                 "mediaPath":"/media/call-1/call.wav","timestamp":"2026-01-01T00:00:00Z"}
                """,
                List.of("callId", "agentId", "templateId", "mediaPath", "timestamp"));
    }

    @Test
    void callTranscriptGenerated() throws Exception {
        assertContract(
                new CallTranscriptGeneratedEvent("call-1", "agent-001", "TEMPLATE_SALES_CALL",
                        "/media/call-1/transcript_v1.json", 1, "model-a"),
                """
                {"callId":"call-1","agentId":"agent-001","templateId":"TEMPLATE_SALES_CALL",
                 "transcriptPath":"/media/call-1/transcript_v1.json","version":1,"modelUsed":"model-a"}
                """,
                List.of("callId", "agentId", "templateId", "transcriptPath", "version", "modelUsed"));
    }

    @Test
    void callSummaryGenerated() throws Exception {
        assertContract(
                new CallSummaryGeneratedEvent("call-1", "agent-001", "TEMPLATE_SALES_CALL",
                        "/media/call-1/summary_v2.json", 2, "model-b"),
                """
                {"callId":"call-1","agentId":"agent-001","templateId":"TEMPLATE_SALES_CALL",
                 "summaryPath":"/media/call-1/summary_v2.json","version":2,"modelUsed":"model-b"}
                """,
                List.of("callId", "agentId", "templateId", "summaryPath", "version", "modelUsed"));
    }

    @Test
    void callEvaluationGenerated() throws Exception {
        assertContract(
                new CallEvaluationGeneratedEvent("call-1", "agent-001", "TEMPLATE_SALES_CALL",
                        "/media/call-1/evaluation_v1.json", 1, "keyword-rules-v1"),
                """
                {"callId":"call-1","agentId":"agent-001","templateId":"TEMPLATE_SALES_CALL",
                 "evaluationPath":"/media/call-1/evaluation_v1.json","version":1,"modelUsed":"keyword-rules-v1"}
                """,
                List.of("callId", "agentId", "templateId", "evaluationPath", "version", "modelUsed"));
    }

    @Test
    void artifactRegenerationRequested() throws Exception {
        assertContract(
                new ArtifactRegenerationRequestedEvent("call-1", "summary", "model-c", "supervisor",
                        "agent-001", "TEMPLATE_SALES_CALL"),
                """
                {"callId":"call-1","artifactType":"summary","requestedModel":"model-c",
                 "requestedBy":"supervisor","agentId":"agent-001","templateId":"TEMPLATE_SALES_CALL"}
                """,
                List.of("callId", "artifactType", "requestedModel", "requestedBy", "agentId", "templateId"));
    }

    @Test
    void artifactRegenerationRequestedAcceptsOldPayloadWithoutOptionalFields() throws Exception {
        ArtifactRegenerationRequestedEvent event = objectMapper.readValue(
                """
                {"callId":"call-1","artifactType":"transcript","requestedModel":null,"requestedBy":"supervisor"}
                """,
                ArtifactRegenerationRequestedEvent.class);

        assertThat(event).isEqualTo(new ArtifactRegenerationRequestedEvent(
                "call-1", "transcript", null, "supervisor", null, null));
    }

    @Test
    void artifactDeleted() throws Exception {
        assertContract(
                new ArtifactDeletedEvent("call-1", "evaluation", 3, "supervisor"),
                """
                {"callId":"call-1","artifactType":"evaluation","version":3,"deletedBy":"supervisor"}
                """,
                List.of("callId", "artifactType", "version", "deletedBy"));
    }

    private <T> void assertContract(T event, String goldenJson, List<String> expectedFieldNames) throws Exception {
        JsonNode serialized = objectMapper.readTree(objectMapper.writeValueAsString(event));
        JsonNode golden = objectMapper.readTree(goldenJson);

        List<String> fieldNames = new ArrayList<>();
        serialized.fieldNames().forEachRemaining(fieldNames::add);
        assertThat(fieldNames).containsExactlyElementsOf(expectedFieldNames);
        assertThat(serialized).isEqualTo(golden);

        @SuppressWarnings("unchecked")
        T roundTripped = (T) objectMapper.readValue(goldenJson, event.getClass());
        assertThat(roundTripped).isEqualTo(event);
    }
}
