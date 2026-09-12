package com.callinsights.evaluationservice.service;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ArtifactVersionResolver {

    private static final Pattern VERSION_PATTERN = Pattern.compile("^.+_v(\\d+)\\.json$");

    public int nextVersion(Path callDirectory, String artifactBaseName) {
        return highestVersioned(callDirectory, artifactBaseName)
                .map(found -> found.version() + 1)
                .orElse(1);
    }

    public Optional<Path> latestArtifactPath(Path callDirectory, String artifactBaseName) {
        return highestVersioned(callDirectory, artifactBaseName).map(Found::path);
    }

    private Optional<Found> highestVersioned(Path callDirectory, String artifactBaseName) {
        if (!Files.isDirectory(callDirectory)) {
            return Optional.empty();
        }
        Found best = null;
        try (DirectoryStream<Path> stream =
                     Files.newDirectoryStream(callDirectory, artifactBaseName + "_v*.json")) {
            for (Path path : stream) {
                Matcher matcher = VERSION_PATTERN.matcher(path.getFileName().toString());
                if (matcher.matches()) {
                    int version = Integer.parseInt(matcher.group(1));
                    if (best == null || version > best.version()) {
                        best = new Found(path, version);
                    }
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to scan " + callDirectory + " for existing " + artifactBaseName + " versions", e);
        }
        return Optional.ofNullable(best);
    }

    private record Found(Path path, int version) {
    }
}
