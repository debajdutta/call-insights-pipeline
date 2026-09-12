package com.callinsights.transcriptionservice.service;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Determines the next artifact version by scanning the filesystem for existing
 * {artifactBaseName}_v{n}.json files next to the media. Day-1 simplification: this does not yet
 * satisfy SPEC.md §4.4's "version numbers are never reused, even after delete" for the
 * delete-then-regenerate case - that needs the Metadata Consumer's Mongo catalog (Task 5) or
 * Gateway (Task 6) to supply the next version instead. Fine for now since nothing publishes
 * artifact-deleted yet.
 */
@Component
public class ArtifactVersionResolver {

    private static final Pattern VERSION_PATTERN = Pattern.compile("^.+_v(\\d+)\\.json$");

    public int nextVersion(Path callDirectory, String artifactBaseName) {
        if (!Files.isDirectory(callDirectory)) {
            return 1;
        }
        int maxVersion = 0;
        try (DirectoryStream<Path> stream =
                     Files.newDirectoryStream(callDirectory, artifactBaseName + "_v*.json")) {
            for (Path path : stream) {
                Matcher matcher = VERSION_PATTERN.matcher(path.getFileName().toString());
                if (matcher.matches()) {
                    maxVersion = Math.max(maxVersion, Integer.parseInt(matcher.group(1)));
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to scan " + callDirectory + " for existing " + artifactBaseName + " versions", e);
        }
        return maxVersion + 1;
    }
}
