package com.callinsights.common.artifact;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

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

    @Test
    void latestArtifactPathIsEmptyForAnEmptyDirectory(@TempDir Path tempDir) {
        assertThat(resolver.latestArtifactPath(tempDir, "transcript")).isEmpty();
    }

    @Test
    void latestArtifactPathIsEmptyWhenCallDirectoryDoesNotExist(@TempDir Path tempDir) {
        Path missing = tempDir.resolve("does-not-exist");

        assertThat(resolver.latestArtifactPath(missing, "transcript")).isEmpty();
    }

    @Test
    void latestArtifactPathPicksTheNumericallyHighestVersion(@TempDir Path tempDir) throws IOException {
        Files.createFile(tempDir.resolve("transcript_v1.json"));
        Files.createFile(tempDir.resolve("transcript_v2.json"));
        Files.createFile(tempDir.resolve("transcript_v10.json"));

        assertThat(resolver.latestArtifactPath(tempDir, "transcript"))
                .isEqualTo(Optional.of(tempDir.resolve("transcript_v10.json")));
    }

    @Test
    void latestArtifactPathIgnoresOtherBaseNames(@TempDir Path tempDir) throws IOException {
        Files.createFile(tempDir.resolve("transcript_v1.json"));
        Files.createFile(tempDir.resolve("summary_v7.json"));
        Files.createFile(tempDir.resolve("evaluation_v9.json"));

        assertThat(resolver.latestArtifactPath(tempDir, "transcript"))
                .isEqualTo(Optional.of(tempDir.resolve("transcript_v1.json")));
    }
}
