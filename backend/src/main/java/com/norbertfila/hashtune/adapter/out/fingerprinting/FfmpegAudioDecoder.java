package com.norbertfila.hashtune.adapter.out.fingerprinting;

import com.norbertfila.hashtune.application.port.out.AudioInputRejectedException;
import com.norbertfila.hashtune.configuration.AudioSafetyProperties;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

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
            byte[] pcmBytes = readProcessOutput(process);
            int exitCode = process.exitValue();
            if (exitCode != 0) {
                throw new IllegalStateException("FFmpeg could not decode audio");
            }
            return Pcm16AudioDecoder.decode(pcmBytes, CanonicalAudioFormat.SAMPLE_RATE);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not decode audio with FFmpeg", exception);
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

    private byte[] readProcessOutput(Process process) {
        try (ExecutorService outputReader = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<byte[]> output = outputReader.submit(() -> {
                try {
                    return BoundedProcessOutput.read(process.getInputStream(), safetyProperties.getMaxDecodedPcmBytes());
                } catch (RuntimeException | IOException exception) {
                    terminate(process);
                    throw exception;
                }
            });
            try {
                if (!process.waitFor(safetyProperties.getFfmpegTimeoutMs(), TimeUnit.MILLISECONDS)) {
                    terminate(process);
                    throw new AudioInputRejectedException("AUDIO_DECODE_TIMEOUT", "Audio decoding timed out");
                }
            } catch (InterruptedException exception) {
                terminate(process);
                Thread.currentThread().interrupt();
                throw new AudioInputRejectedException("AUDIO_DECODE_TIMEOUT", "Audio decoding was interrupted");
            }
            try {
                return output.get();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                terminate(process);
                throw new AudioInputRejectedException("AUDIO_DECODE_TIMEOUT", "Audio decoding was interrupted");
            } catch (ExecutionException exception) {
                Throwable cause = exception.getCause();
                if (cause instanceof RuntimeException runtimeException) {
                    throw runtimeException;
                }
                throw new IllegalStateException("Could not read FFmpeg output", cause);
            }
        }
    }

    private void terminate(Process process) {
        if (process.isAlive()) {
            process.destroy();
            if (process.isAlive()) {
                process.destroyForcibly();
            }
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
