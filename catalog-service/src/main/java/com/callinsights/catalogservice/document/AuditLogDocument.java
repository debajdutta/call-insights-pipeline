package com.callinsights.catalogservice.document;

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

    public AuditLogDocument() {
    }

    public AuditLogDocument(String callId, String artifactType, String action, Integer version,
                             String modelUsed, String actor, String timestamp) {
        this.callId = callId;
        this.artifactType = artifactType;
        this.action = action;
        this.version = version;
        this.modelUsed = modelUsed;
        this.actor = actor;
        this.timestamp = timestamp;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCallId() {
        return callId;
    }

    public void setCallId(String callId) {
        this.callId = callId;
    }

    public String getArtifactType() {
        return artifactType;
    }

    public void setArtifactType(String artifactType) {
        this.artifactType = artifactType;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getModelUsed() {
        return modelUsed;
    }

    public void setModelUsed(String modelUsed) {
        this.modelUsed = modelUsed;
    }

    public String getActor() {
        return actor;
    }

    public void setActor(String actor) {
        this.actor = actor;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}
