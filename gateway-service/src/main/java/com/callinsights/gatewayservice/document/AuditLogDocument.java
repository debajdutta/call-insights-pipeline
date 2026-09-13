package com.callinsights.gatewayservice.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "audit_log")
public class AuditLogDocument {

    @Id
    private String id;
    private String callId;
    private String artifactType;
    private String action;
    private Integer version;
    private String modelUsed;
    private String actor;
    private String timestamp;

    public String getId() {
        return id;
    }

    public String getCallId() {
        return callId;
    }

    public String getArtifactType() {
        return artifactType;
    }

    public String getAction() {
        return action;
    }

    public Integer getVersion() {
        return version;
    }

    public String getModelUsed() {
        return modelUsed;
    }

    public String getActor() {
        return actor;
    }

    public String getTimestamp() {
        return timestamp;
    }
}
