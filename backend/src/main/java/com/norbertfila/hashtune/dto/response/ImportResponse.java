package com.norbertfila.hashtune.dto.response;

import java.util.UUID;

public record ImportResponse(String status, UUID trackId, UUID indexingJobId, TrackResponse track) {}
