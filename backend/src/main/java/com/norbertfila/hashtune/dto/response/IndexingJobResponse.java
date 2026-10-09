package com.norbertfila.hashtune.dto.response;

import java.time.Instant;
import java.util.UUID;

public record IndexingJobResponse(
        UUID id,
        UUID trackId,
        String status,
        int progress,
        int attempts,
        Instant nextAttemptAt,
        String errorCode,
        String errorMessage) {}
