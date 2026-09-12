package com.callinsights.callgenerator.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class MediaFileGeneratorTest {

    private final MediaFileGenerator generator = new MediaFileGenerator();

    @Test
    void generatesAPlayableWavFileWithTheExpectedFormat(@TempDir Path tempDir) throws IOException, UnsupportedAudioFileException {
        Path callDirectory = tempDir.resolve("some-call-id");

        Path result = generator.generate(callDirectory, "call.wav");

        assertThat(Files.exists(result)).isTrue();

        AudioFileFormat fileFormat = AudioSystem.getAudioFileFormat(result.toFile());
        assertThat(fileFormat.getType()).isEqualTo(AudioFileFormat.Type.WAVE);

        AudioFormat format = fileFormat.getFormat();
        assertThat(format.getSampleRate()).isEqualTo(8000f);
        assertThat(format.getChannels()).isEqualTo(1);
        assertThat(format.getSampleSizeInBits()).isEqualTo(16);
    }

    @Test
    void createsParentDirectoriesThatDoNotYetExist(@TempDir Path tempDir) throws IOException {
        Path callDirectory = tempDir.resolve("nested").resolve("call-id");

        generator.generate(callDirectory, "call.wav");

        assertThat(Files.isDirectory(callDirectory)).isTrue();
    }
}
