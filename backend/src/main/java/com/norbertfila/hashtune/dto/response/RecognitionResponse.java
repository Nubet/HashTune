package com.norbertfila.hashtune.dto.response;

public record RecognitionResponse(
        String status,
        TrackResponse track,
        Double confidence,
        Long matchedAtMs,
        Long sampleDurationMs,
        Long recognitionTimeMs) {}
