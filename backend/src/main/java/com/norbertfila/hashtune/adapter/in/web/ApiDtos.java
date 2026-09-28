package com.norbertfila.hashtune.adapter.in.web;

import com.norbertfila.hashtune.domain.indexing.IndexingJob;
import com.norbertfila.hashtune.domain.recognition.Recognition;
import com.norbertfila.hashtune.domain.track.Track;
import java.time.Instant;
import java.util.UUID;

public final class ApiDtos {
    private ApiDtos() { }

    public record TrackResponse(UUID id, String title, String artist, String album, Long durationMs, String status, Instant createdAt) {
        static TrackResponse from(Track track) {
            return new TrackResponse(track.id(), track.title(), track.artist(), track.album(), track.durationMs(), track.status().name(), track.createdAt());
        }
    }

    public record UploadResponse(UUID trackId, UUID indexingJobId, String status) {
        static UploadResponse from(Track track, IndexingJob job) {
            return new UploadResponse(track.id(), job.id(), job.status().name());
        }
    }

    public record IndexingJobResponse(UUID id, UUID trackId, String status, int progress, String errorCode, String errorMessage) {
        static IndexingJobResponse from(IndexingJob job) {
            return new IndexingJobResponse(job.id(), job.trackId(), job.status().name(), job.progress(), job.errorCode(), job.errorMessage());
        }
    }

    public record RecognitionResponse(String status, TrackResponse track, Double confidence, Long matchedAtMs, Long sampleDurationMs, Long recognitionTimeMs) {
        static RecognitionResponse from(Recognition recognition, Track track) {
            return new RecognitionResponse(recognition.status().name(), track == null ? null : TrackResponse.from(track), recognition.confidence(), recognition.matchedAtMs(), recognition.sampleDurationMs(), recognition.recognitionTimeMs());
        }
    }

    public record HistoryResponse(UUID id, TrackResponse track, String status, Double confidence, String source, Instant createdAt) {
        static HistoryResponse from(Recognition recognition, Track track) {
            return new HistoryResponse(recognition.id(), track == null ? null : TrackResponse.from(track), recognition.status().name(), recognition.confidence(), recognition.source().name(), recognition.createdAt());
        }
    }

    public record ProblemResponse(String type, String title, int status, String detail, String instance) { }
}
