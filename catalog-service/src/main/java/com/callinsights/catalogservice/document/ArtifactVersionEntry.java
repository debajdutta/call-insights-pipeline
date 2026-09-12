package com.callinsights.catalogservice.document;

public class ArtifactVersionEntry {

    private int version;
    private String path;
    private String modelUsed;
    private String generatedAt;

    public ArtifactVersionEntry() {
    }

    public ArtifactVersionEntry(int version, String path, String modelUsed, String generatedAt) {
        this.version = version;
        this.path = path;
        this.modelUsed = modelUsed;
        this.generatedAt = generatedAt;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getModelUsed() {
        return modelUsed;
    }

    public void setModelUsed(String modelUsed) {
        this.modelUsed = modelUsed;
    }

    public String getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(String generatedAt) {
        this.generatedAt = generatedAt;
    }
}
