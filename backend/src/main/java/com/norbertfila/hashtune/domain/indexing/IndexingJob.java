package com.norbertfila.hashtune.domain.indexing;

import java.time.Instant;
import java.util.UUID;

public record IndexingJob(
        UUID id,
        UUID trackId,
        IndexingJobStatus status,
        int progress,
        int attempts,
        String errorCode,
        String errorMessage,
        Instant createdAt,
        Instant startedAt,
        Instant finishedAt) {}
