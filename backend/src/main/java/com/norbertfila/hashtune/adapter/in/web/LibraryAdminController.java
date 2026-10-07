package com.norbertfila.hashtune.adapter.in.web;

import com.norbertfila.hashtune.application.service.TrackApplicationService;
import com.norbertfila.hashtune.domain.track.TrackOrigin;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@PreAuthorize("hasRole('ADMIN')")
public class LibraryAdminController {
    private final TrackApplicationService service;

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<ApiDtos.UploadResponse> upload(@RequestPart("file") MultipartFile file) {
        TrackApplicationService.UploadResult result = service.upload(file);
        return ResponseEntity.accepted().body(ApiDtos.UploadResponse.from(result.track(), result.job()));
    }

    @PostMapping(value = "/imports", consumes = "multipart/form-data")
    public ApiDtos.ImportResponse importTrack(
            @RequestPart("file") MultipartFile file,
            @RequestParam(defaultValue = "HASH_TUNE") TrackOrigin origin,
            @RequestParam(required = false) String relativePath) {
        TrackApplicationService.ImportResult result = service.importTrack(file, origin, relativePath);
        return ApiDtos.ImportResponse.from(result.track(), result);
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

    @PostMapping("/reindex")
    public ResponseEntity<ApiDtos.ReindexAllResponse> reindexAll() {
        TrackApplicationService.ReindexAllResult result = service.reindexAll();
        return ResponseEntity.accepted()
                .body(new ApiDtos.ReindexAllResponse(
                        result.scheduled(), result.alreadyProcessing(), result.awaitingConfirmation()));
    }
}
