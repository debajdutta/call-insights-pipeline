package com.callinsights.catalogservice.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

/**
 * One document per (callId, artifactType). _id is "<callId>:<artifactType>" so upserts by
 * callId+artifactType are a single findById/save round trip rather than a query.
 */
@Document(collection = "artifacts")
public class ArtifactCatalogDocument {

    @Id
    private String id;
    private String callId;
    private String artifactType;
    private Integer currentVersion;
    private boolean deleted;
    private List<ArtifactVersionEntry> versions = new ArrayList<>();

    public ArtifactCatalogDocument() {
    }

    public static String buildId(String callId, String artifactType) {
        return callId + ":" + artifactType;
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

    public Integer getCurrentVersion() {
        return currentVersion;
    }

    public void setCurrentVersion(Integer currentVersion) {
        this.currentVersion = currentVersion;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public List<ArtifactVersionEntry> getVersions() {
        return versions;
    }

    public void setVersions(List<ArtifactVersionEntry> versions) {
        this.versions = versions;
    }
}
