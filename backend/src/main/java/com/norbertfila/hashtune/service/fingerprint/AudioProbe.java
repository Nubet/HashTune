package com.norbertfila.hashtune.service.fingerprint;

import com.norbertfila.hashtune.exceptions.ErrorCode;
import com.norbertfila.hashtune.exceptions.audio.AudioInputRejectedException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.TimeUnit;

final class AudioProbe {
    private final String ffprobeBinary;
    private final long timeoutMs;

    AudioProbe(String ffprobeBinary, long timeoutMs) {
        this.ffprobeBinary = ffprobeBinary;
        this.timeoutMs = timeoutMs;
    }

    Result inspect(Path inputFile) {
        Process process = null;
        try {
            process = new ProcessBuilder(
                            ffprobeBinary,
                            "-v",
                            "error",
                            "-select_streams",
                            "a:0",
                            "-show_entries",
                            "stream=codec_type,duration:format=format_name,duration",
                            "-of",
                            "default=noprint_wrappers=1",
                            inputFile.toString())
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start();
            if (!process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
                throw new AudioInputRejectedException(
                        ErrorCode.AUDIO_PROBE_TIMEOUT, "Audio format inspection timed out");
            }
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (process.exitValue() != 0) {
                throw unsupportedFormat();
            }
            return parseOutput(output);
        } catch (IOException exception) {
            throw new AudioInputRejectedException(
                    ErrorCode.AUDIO_FORMAT_NOT_SUPPORTED, "Audio format could not be inspected");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AudioInputRejectedException(
                    ErrorCode.AUDIO_PROBE_TIMEOUT, "Audio format inspection was interrupted");
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    static Result parseOutput(String output) {
        Set<String> containerNames = new java.util.HashSet<>();
        boolean hasAudio = false;
        double durationSeconds = -1;
        for (String line : output.split("\\R")) {
            String[] entry = line.split("=", 2);
            if (entry.length != 2) {
                continue;
            }
            switch (entry[0]) {
                case "codec_type" -> hasAudio |= "audio".equalsIgnoreCase(entry[1].trim());
                case "format_name" ->
                    containerNames.addAll(
                            Arrays.asList(entry[1].trim().toLowerCase().split(",")));
                case "duration" -> durationSeconds = Math.max(durationSeconds, parseDuration(entry[1]));
                default -> {
                    // Ignore probe fields that are not part of the safety contract.
                }
            }
        }
        if (!hasAudio || durationSeconds <= 0) {
            throw unsupportedFormat();
        }
        return new Result(containerNames, Math.round(durationSeconds * 1_000));
    }

    private static double parseDuration(String value) {
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private static AudioInputRejectedException unsupportedFormat() {
        return new AudioInputRejectedException(
                ErrorCode.AUDIO_FORMAT_NOT_SUPPORTED, "Audio must contain a supported audio stream");
    }

    record Result(Set<String> containerNames, long durationMs) {}
}
