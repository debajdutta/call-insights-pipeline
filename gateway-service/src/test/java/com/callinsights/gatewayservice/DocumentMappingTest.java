package com.callinsights.gatewayservice;

import com.callinsights.gatewayservice.document.ArtifactCatalogDocument;
import com.callinsights.gatewayservice.document.ArtifactVersionEntry;
import com.callinsights.gatewayservice.document.AuditLogDocument;
import com.callinsights.gatewayservice.document.CallDocument;
import org.junit.jupiter.api.Test;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the Mongo mapping of the document classes that catalog-service and gateway-service each
 * keep a copy of (M0b decision 5). The same expectations live in both services' copy of this
 * test, so if one copy drifts from the shared collection layout, that service's test fails.
 */
class DocumentMappingTest {

    @Test
    void callDocument() {
        assertMapping(CallDocument.class, "calls", "callId",
                List.of("callId:String", "agentId:String", "templateId:String",
                        "mediaPath:String", "timestamp:String"));
    }

    @Test
    void auditLogDocument() {
        assertMapping(AuditLogDocument.class, "audit_log", "id",
                List.of("id:String", "callId:String", "artifactType:String", "action:String",
                        "version:Integer", "modelUsed:String", "actor:String", "timestamp:String"));
    }

    @Test
    void artifactCatalogDocument() {
        assertMapping(ArtifactCatalogDocument.class, "artifacts", "id",
                List.of("id:String", "callId:String", "artifactType:String", "currentVersion:Integer",
                        "deleted:boolean", "versions:List"));
    }

    @Test
    void artifactVersionEntryIsEmbedded() {
        assertThat(ArtifactVersionEntry.class.getAnnotation(Document.class)).isNull();
        assertThat(fields(ArtifactVersionEntry.class)).containsExactly(
                "version:int", "path:String", "modelUsed:String", "generatedAt:String");
    }

    private static void assertMapping(Class<?> type, String collection, String idField, List<String> expected) {
        Document document = type.getAnnotation(Document.class);
        assertThat(document).as("@Document on %s", type.getSimpleName()).isNotNull();
        assertThat(document.collection()).isEqualTo(collection);
        assertThat(Arrays.stream(type.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(Id.class))
                .map(Field::getName))
                .containsExactly(idField);
        assertThat(fields(type)).containsExactlyElementsOf(expected);
    }

    private static List<String> fields(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields())
                .filter(f -> !Modifier.isStatic(f.getModifiers()) && !f.isSynthetic())
                .map(f -> f.getName() + ":" + f.getType().getSimpleName())
                .toList();
    }
}
