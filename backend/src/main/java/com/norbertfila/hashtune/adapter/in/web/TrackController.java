package com.norbertfila.hashtune.adapter.in.web;

import com.norbertfila.hashtune.application.service.TrackApplicationService;
import com.norbertfila.hashtune.domain.track.TrackOrigin;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/library/tracks")
@RequiredArgsConstructor
public class TrackController {
    private final TrackApplicationService service;

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<ApiDtos.UploadResponse> upload(@RequestPart("file") MultipartFile file) {
        TrackApplicationService.UploadResult result = service.upload(file);
        return ResponseEntity.accepted().body(ApiDtos.UploadResponse.from(result.track(), result.job()));
    }

    @PostMapping(value = "/imports", consumes = "multipart/form-data")
    public ApiDtos.ImportResponse importTrack(
            @RequestPart("file") MultipartFile file,
            @RequestParam(defaultValue = "PERSONAL") TrackOrigin origin,
            @RequestParam(required = false) String relativePath) {
        TrackApplicationService.ImportResult result = service.importTrack(file, origin, relativePath);
        return ApiDtos.ImportResponse.from(result.track(), result);
    }

    @GetMapping
    public List<ApiDtos.TrackResponse> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) TrackOrigin origin,
            @RequestParam(defaultValue = "25") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        return service.search(query, origin, limit, offset).stream()
                .map(ApiDtos.TrackResponse::from)
                .toList();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/metadata")
    public ApiDtos.TrackResponse updateMetadata(
            @PathVariable UUID id, @RequestBody ApiDtos.UpdateTrackMetadataRequest request) {
        return ApiDtos.TrackResponse.from(
                service.updateMetadata(id, request.title(), request.artist(), request.album()));
    }

    @PostMapping("/{id}/reindex")
    public ResponseEntity<ApiDtos.IndexingJobResponse> reindex(@PathVariable UUID id) {
        return ResponseEntity.accepted().body(ApiDtos.IndexingJobResponse.from(service.reindex(id)));
    }

    @GetMapping("/{id}/cover")
    public ResponseEntity<byte[]> cover(@PathVariable UUID id) {
        TrackApplicationService.CoverArt cover = service.getCoverArt(id);
        MediaType mediaType = MediaType.parseMediaType(
                cover.mimeType() == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : cover.mimeType());
        return ResponseEntity.ok().contentType(mediaType).body(cover.data());
    }
}
