package com.norbertfila.hashtune.adapter.in.web;

import com.norbertfila.hashtune.application.port.out.TrackSearchQuery;
import com.norbertfila.hashtune.application.service.ArtistImageService;
import com.norbertfila.hashtune.application.service.TrackApplicationService;
import com.norbertfila.hashtune.application.service.TrackQueryService;
import com.norbertfila.hashtune.domain.track.TrackOrigin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.InputStreamResource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v1/library/tracks")
@RequiredArgsConstructor
public class LibraryQueryController {
    private final TrackQueryService queryService;
    private final ArtistImageService artistImages;
    private final TrackApplicationService service;

    @GetMapping
    public ApiDtos.PageResponse<ApiDtos.TrackResponse> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) TrackOrigin origin,
            @RequestParam(required = false) String artist,
            @RequestParam(required = false) String album,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "25") @Min(1) @Max(100) int size) {
        return ApiDtos.PageResponse.from(
                queryService.searchTracks(new TrackSearchQuery(query, origin, artist, album, page, size)),
                ApiDtos.TrackResponse::from);
    }

    @GetMapping("/albums")
    public ApiDtos.PageResponse<ApiDtos.AlbumResponse> albums(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) TrackOrigin origin,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "25") @Min(1) @Max(100) int size) {
        return ApiDtos.PageResponse.from(
                queryService.searchAlbums(query, origin, page, size), ApiDtos.AlbumResponse::from);
    }

    @GetMapping("/artists")
    public ApiDtos.PageResponse<ApiDtos.ArtistResponse> artists(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) TrackOrigin origin,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "25") @Min(1) @Max(100) int size) {
        return ApiDtos.PageResponse.from(
                queryService.searchArtists(query, origin, page, size), ApiDtos.ArtistResponse::from);
    }

    @GetMapping("/artists/{artistName}/image")
    public ResponseEntity<Void> artistImage(@PathVariable String artistName) {
        var imageUrl = artistImages.findImageForDelivery(artistName);
        if (imageUrl.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(imageUrl.get()))
                .build();
    }

    @GetMapping("/{id}/cover")
    public ResponseEntity<InputStreamResource> cover(@PathVariable UUID id) {
        TrackApplicationService.CoverArt cover = service.getCoverArt(id);
        MediaType mediaType = MediaType.parseMediaType(cover.mimeType());
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header("X-Content-Type-Options", "nosniff")
                .body(new InputStreamResource(cover.content()));
    }
}
