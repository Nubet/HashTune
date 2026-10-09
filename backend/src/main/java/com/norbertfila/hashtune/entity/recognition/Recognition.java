package com.norbertfila.hashtune.entity.recognition;

import com.norbertfila.hashtune.entity.identity.ExternalIdentity;
import java.time.Instant;
import java.util.UUID;

public record Recognition(
        UUID id,
        ExternalIdentity owner,
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
