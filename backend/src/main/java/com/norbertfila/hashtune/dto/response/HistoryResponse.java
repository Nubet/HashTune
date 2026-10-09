package com.norbertfila.hashtune.dto.response;

import java.time.Instant;
import java.util.UUID;

public record HistoryResponse(
        UUID id,
        TrackResponse track,
        String status,
        Double confidence,
        String source,
        String recordingUrl,
        String downloadUrl,
        Long sampleDurationMs,
        Instant createdAt) {}
