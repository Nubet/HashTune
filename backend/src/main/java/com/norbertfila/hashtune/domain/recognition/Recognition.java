package com.norbertfila.hashtune.domain.recognition;

import java.time.Instant;
import java.util.UUID;

public record Recognition(UUID id, UUID trackId, RecognitionStatus status, Double confidence,
                          Long matchedAtMs, RecognitionSource source, Long sampleDurationMs,
                          Long recognitionTimeMs, Instant createdAt) { }
