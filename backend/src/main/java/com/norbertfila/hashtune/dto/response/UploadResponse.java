package com.norbertfila.hashtune.dto.response;

import java.util.UUID;

public record UploadResponse(UUID trackId, UUID indexingJobId, String status, TrackResponse track) {}
