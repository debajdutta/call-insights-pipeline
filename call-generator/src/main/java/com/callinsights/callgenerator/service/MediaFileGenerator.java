package com.callinsights.callgenerator.service;

import org.springframework.stereotype.Component;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Writes a short silent WAV file as a stand-in for real call-recording media.
 * Built entirely on javax.sound.sampled (JDK built-in) so no audio-codec
 * dependency is needed for a file whose only purpose is to exist and be
 * referenced by downstream artifact generation.
 */
@Component
public class MediaFileGenerator {

    private static final float SAMPLE_RATE = 8000f;
    private static final int SAMPLE_SIZE_BITS = 16;
    private static final int CHANNELS = 1;
    private static final double DURATION_SECONDS = 2.0;

    public Path generate(Path targetDirectory, String fileName) throws IOException {
        Files.createDirectories(targetDirectory);
        Path targetFile = targetDirectory.resolve(fileName);

        AudioFormat format = new AudioFormat(SAMPLE_RATE, SAMPLE_SIZE_BITS, CHANNELS, true, false);
        int frameCount = (int) (SAMPLE_RATE * DURATION_SECONDS);
        byte[] silence = new byte[frameCount * format.getFrameSize()];

        try (AudioInputStream audioInputStream = new AudioInputStream(
                new ByteArrayInputStream(silence), format, frameCount)) {
            AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, targetFile.toFile());
        }

        return targetFile;
    }
}
