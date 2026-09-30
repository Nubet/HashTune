package com.norbertfila.hashtune.adapter.in.web;

import com.norbertfila.hashtune.application.port.out.AlbumSummary;
import com.norbertfila.hashtune.application.port.out.ArtistSummary;
import com.norbertfila.hashtune.application.port.out.PageResult;
import com.norbertfila.hashtune.domain.indexing.IndexingJob;
import com.norbertfila.hashtune.domain.recognition.Recognition;
import com.norbertfila.hashtune.domain.track.Track;
import com.norbertfila.hashtune.domain.track.TrackOrigin;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

public final class ApiDtos {
    private ApiDtos() {}

    public record TrackResponse(
            UUID id,
            String title,
            String artist,
            String album,
            TrackOrigin origin,
            String albumArtist,
            String composer,
            String genre,
            String releaseYear,
            Integer trackNumber,
            Integer discNumber,
            String isrc,
            String barcode,
            String comment,
            String coverArtUrl,
            Long durationMs,
            String status,
            Instant createdAt) {
        static TrackResponse from(Track track) {
            return new TrackResponse(
                    track.id(),
                    track.title(),
                    track.artist(),
                    track.album(),
                    track.origin(),
                    track.albumArtist(),
                    track.composer(),
                    track.genre(),
                    track.releaseYear(),
                    track.trackNumber(),
                    track.discNumber(),
                    track.isrc(),
                    track.barcode(),
                    track.comment(),
                    track.coverArtObjectKey() == null
                            ? track.coverArtUrl()
                            : ServletUriComponentsBuilder.fromCurrentContextPath()
                                    .path("/api/v1/library/tracks/{id}/cover")
                                    .buildAndExpand(track.id())
                                    .toUriString(),
                    track.durationMs(),
                    track.status().name(),
                    track.createdAt());
        }
    }

    public record UploadResponse(UUID trackId, UUID indexingJobId, String status, TrackResponse track) {
        static UploadResponse from(Track track, IndexingJob job) {
            return new UploadResponse(track.id(), job.id(), job.status().name(), TrackResponse.from(track));
        }
    }

    public record AlbumResponse(String title, String artist, String coverArtUrl, long trackCount) {
        static AlbumResponse from(AlbumSummary album) {
            String coverArtUrl = album.coverArtUrl();
            if (coverArtUrl == null && album.coverArtTrackId() != null) {
                coverArtUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                        .path("/api/v1/library/tracks/{id}/cover")
                        .buildAndExpand(album.coverArtTrackId())
                        .toUriString();
            }
            return new AlbumResponse(album.title(), album.artist(), coverArtUrl, album.trackCount());
        }
    }

    public record ArtistResponse(String name, long trackCount, long albumCount) {
        static ArtistResponse from(ArtistSummary artist) {
            return new ArtistResponse(artist.name(), artist.trackCount(), artist.albumCount());
        }
    }

    public record PageResponse<T>(
            List<T> content,
            int page,
            int size,
            long totalElements,
            int totalPages,
            boolean hasNext,
            boolean hasPrevious) {
        static <T, R> PageResponse<R> from(PageResult<T> result, Function<T, R> mapper) {
            return new PageResponse<>(
                    result.content().stream().map(mapper).toList(),
                    result.page(),
                    result.size(),
                    result.totalElements(),
                    result.totalPages(),
                    result.hasNext(),
                    result.hasPrevious());
        }
    }

    public record ImportResponse(String status, UUID trackId, UUID indexingJobId, TrackResponse track) {
        static ImportResponse from(
                Track track, com.norbertfila.hashtune.application.service.TrackApplicationService.ImportResult result) {
            return new ImportResponse(
                    result.status(),
                    track == null ? null : track.id(),
                    result.job() == null ? null : result.job().id(),
                    track == null ? null : TrackResponse.from(track));
        }
    }

    public record UpdateTrackMetadataRequest(String title, String artist, String album) {}

    public record IndexingJobResponse(
            UUID id, UUID trackId, String status, int progress, String errorCode, String errorMessage) {
        static IndexingJobResponse from(IndexingJob job) {
            return new IndexingJobResponse(
                    job.id(), job.trackId(), job.status().name(), job.progress(), job.errorCode(), job.errorMessage());
        }
    }

    public record RecognitionResponse(
            String status,
            TrackResponse track,
            Double confidence,
            Long matchedAtMs,
            Long sampleDurationMs,
            Long recognitionTimeMs) {
        static RecognitionResponse from(Recognition recognition, Track track) {
            return new RecognitionResponse(
                    recognition.status().name(),
                    track == null ? null : TrackResponse.from(track),
                    recognition.confidence(),
                    recognition.matchedAtMs(),
                    recognition.sampleDurationMs(),
                    recognition.recognitionTimeMs());
        }
    }

    public record HistoryResponse(
            UUID id, TrackResponse track, String status, Double confidence, String source, Instant createdAt) {
        static HistoryResponse from(Recognition recognition, Track track) {
            return new HistoryResponse(
                    recognition.id(),
                    track == null ? null : TrackResponse.from(track),
                    recognition.status().name(),
                    recognition.confidence(),
                    recognition.source().name(),
                    recognition.createdAt());
        }
    }

    public record ProblemResponse(
            String type,
            String title,
            int status,
            String detail,
            String instance,
            String code,
            Map<String, String> errors) {}
}
