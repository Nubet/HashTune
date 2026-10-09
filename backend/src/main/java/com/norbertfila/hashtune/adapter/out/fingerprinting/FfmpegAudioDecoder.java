package com.norbertfila.hashtune.adapter.out.fingerprinting;

import com.norbertfila.hashtune.application.port.out.AudioInputRejectedException;
import com.norbertfila.hashtune.configuration.AudioSafetyProperties;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

final class FfmpegAudioDecoder {
    private final String ffmpegBinary;
    private final AudioProbe probe;
    private final AudioSafetyProperties safetyProperties;

    FfmpegAudioDecoder(String ffmpegBinary, String ffprobeBinary, AudioSafetyProperties safetyProperties) {
        this.ffmpegBinary = ffmpegBinary;
        this.probe = new AudioProbe(ffprobeBinary, safetyProperties.getProbeTimeoutMs());
        this.safetyProperties = safetyProperties;
    }

    DecodedAudio decode(InputStream audio) {
        Path inputFile = null;
        try {
            inputFile = Files.createTempFile("hashtune-audio-", ".input");
            Files.copy(audio, inputFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            validate(probe.inspect(inputFile));

            Process process = new ProcessBuilder(command(inputFile))
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start();
            byte[] pcmBytes = readAll(process.getInputStream());
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IllegalStateException("FFmpeg could not decode audio");
            }
            return Pcm16AudioDecoder.decode(pcmBytes, CanonicalAudioFormat.SAMPLE_RATE);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not decode audio with FFmpeg", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("FFmpeg decoding was interrupted", exception);
        } finally {
            deleteInputFile(inputFile);
        }
    }

    private void validate(AudioProbe.Result result) {
        boolean supportedContainer = result.containerNames().stream()
                .anyMatch(safetyProperties.getAllowedContainerNames()::contains);
        if (!supportedContainer) {
            throw new AudioInputRejectedException(
                    "AUDIO_FORMAT_NOT_SUPPORTED", "Audio container is not supported");
        }
        if (result.durationMs() > safetyProperties.getMaxDurationMs()) {
            throw new AudioInputRejectedException(
                    "AUDIO_DURATION_TOO_LONG", "Audio exceeds the maximum allowed duration");
        }
    }

    private List<String> command(Path inputFile) {
        return List.of(
                ffmpegBinary,
                "-hide_banner",
                "-loglevel",
                "error",
                "-i",
                inputFile.toString(),
                "-f",
                CanonicalAudioFormat.FFMPEG_OUTPUT_FORMAT,
                "-acodec",
                CanonicalAudioFormat.FFMPEG_AUDIO_CODEC,
                "-ac",
                String.valueOf(CanonicalAudioFormat.CHANNEL_COUNT),
                "-ar",
                String.valueOf(CanonicalAudioFormat.SAMPLE_RATE),
                CanonicalAudioFormat.FFMPEG_OUTPUT_PIPE);
    }

    private byte[] readAll(InputStream input) throws IOException {
        try (input;
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            input.transferTo(output);
            return output.toByteArray();
        }
    }

    private void deleteInputFile(Path inputFile) {
        if (inputFile == null) {
            return;
        }
        try {
            Files.deleteIfExists(inputFile);
        } catch (IOException ignored) {
            // Temporary-file cleanup must not hide the decoding result
        }
    }
}
