package com.norbertfila.hashtune.service.fingerprint;

import java.util.UUID;

public interface AudioRecognitionEngine {
    IndexingResult index(UUID trackId, InputAudio audio);

    RecognitionResult recognize(InputAudio audio);

    record InputAudio(String bucket, String objectKey, String fileName, String checksum) {}

    record IndexingResult(long durationMs, int fingerprintCount) {}

    record RecognitionResult(
            boolean matched,
            UUID trackId,
            double confidence,
            long matchedAtMs,
            long sampleDurationMs,
            int hashMatches,
            int offsetClusterSize) {
        public static RecognitionResult noMatch(long sampleDurationMs) {
            return new RecognitionResult(false, null, 0, 0, sampleDurationMs, 0, 0);
        }
    }
}
