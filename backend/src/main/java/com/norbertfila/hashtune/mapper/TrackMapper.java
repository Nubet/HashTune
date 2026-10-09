package com.norbertfila.hashtune.mapper;

import com.norbertfila.hashtune.dto.response.ImportResponse;
import com.norbertfila.hashtune.dto.response.TrackResponse;
import com.norbertfila.hashtune.dto.response.UploadResponse;
import com.norbertfila.hashtune.entity.indexing.IndexingJob;
import com.norbertfila.hashtune.entity.track.Track;
import com.norbertfila.hashtune.service.track.TrackService;
import java.util.UUID;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

public final class TrackMapper {
    private TrackMapper() {}

    public static TrackResponse toResponse(Track track) {
        String coverArtUrl = track.coverArtObjectKey() == null
                ? track.coverArtUrl()
                : ServletUriComponentsBuilder.fromCurrentContextPath()
                        .path("/api/v1/library/tracks/{id}/cover")
                        .buildAndExpand(track.id())
                        .toUriString();
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
                coverArtUrl,
                track.durationMs(),
                track.status().name(),
                track.createdAt());
    }

    public static UploadResponse toUploadResponse(Track track, IndexingJob job) {
        return new UploadResponse(track.id(), job.id(), job.status().name(), toResponse(track));
    }

    public static ImportResponse toImportResponse(Track track, TrackService.ImportResult result) {
        return new ImportResponse(
                result.status(),
                track == null ? null : track.id(),
                result.job() == null ? null : result.job().id(),
                track == null ? null : toResponse(track));
    }

    public static String coverUrl(UUID trackId) {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/library/tracks/{id}/cover")
                .buildAndExpand(trackId)
                .toUriString();
    }
}
