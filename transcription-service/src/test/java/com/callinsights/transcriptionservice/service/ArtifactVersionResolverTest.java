package com.callinsights.transcriptionservice.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ArtifactVersionResolverTest {

    private final ArtifactVersionResolver resolver = new ArtifactVersionResolver();

    @Test
    void returnsOneWhenCallDirectoryDoesNotExist(@TempDir Path tempDir) {
        Path missing = tempDir.resolve("does-not-exist");

        assertThat(resolver.nextVersion(missing, "transcript")).isEqualTo(1);
    }

    @Test
    void returnsOneWhenDirectoryHasNoMatchingArtifacts(@TempDir Path tempDir) throws IOException {
        Files.createFile(tempDir.resolve("call.wav"));

        assertThat(resolver.nextVersion(tempDir, "transcript")).isEqualTo(1);
    }

    @Test
    void incrementsPastTheHighestExistingVersion(@TempDir Path tempDir) throws IOException {
        Files.createFile(tempDir.resolve("transcript_v1.json"));
        Files.createFile(tempDir.resolve("transcript_v2.json"));

        assertThat(resolver.nextVersion(tempDir, "transcript")).isEqualTo(3);
    }

    @Test
    void handlesGapsInExistingVersions(@TempDir Path tempDir) throws IOException {
        Files.createFile(tempDir.resolve("transcript_v1.json"));
        Files.createFile(tempDir.resolve("transcript_v3.json"));

        assertThat(resolver.nextVersion(tempDir, "transcript")).isEqualTo(4);
    }

    @Test
    void ignoresArtifactsOfADifferentType(@TempDir Path tempDir) throws IOException {
        Files.createFile(tempDir.resolve("summary_v5.json"));

        assertThat(resolver.nextVersion(tempDir, "transcript")).isEqualTo(1);
    }
}
