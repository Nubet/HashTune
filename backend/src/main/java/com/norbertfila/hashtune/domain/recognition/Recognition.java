package com.norbertfila.hashtune.domain.recognition;

import com.norbertfila.hashtune.domain.session.ClientSessionId;
import java.time.Instant;
import java.util.UUID;

public record Recognition(
        UUID id,
        ClientSessionId sessionId,
        UUID trackId,
        RecognitionStatus status,
        Double confidence,
        Long matchedAtMs,
        RecognitionSource source,
        Long sampleDurationMs,
        Long recognitionTimeMs,
        String recordingObjectKey,
        String recordingContentType,
        String recordingFileName,
        Instant createdAt) {}
