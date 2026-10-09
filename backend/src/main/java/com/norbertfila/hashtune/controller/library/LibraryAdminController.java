package com.norbertfila.hashtune.controller.library;

import com.norbertfila.hashtune.dto.request.UpdateTrackMetadataRequest;
import com.norbertfila.hashtune.dto.response.ImportResponse;
import com.norbertfila.hashtune.dto.response.IndexingJobResponse;
import com.norbertfila.hashtune.dto.response.ReindexAllResponse;
import com.norbertfila.hashtune.dto.response.TrackResponse;
import com.norbertfila.hashtune.dto.response.UploadResponse;
import com.norbertfila.hashtune.entity.track.TrackOrigin;
import com.norbertfila.hashtune.mapper.IndexingJobMapper;
import com.norbertfila.hashtune.mapper.LibraryMapper;
import com.norbertfila.hashtune.mapper.TrackMapper;
import com.norbertfila.hashtune.service.track.TrackService;
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
    private final TrackService service;

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<UploadResponse> upload(@RequestPart("file") MultipartFile file) {
        TrackService.UploadResult result = service.upload(file);
        return ResponseEntity.accepted().body(TrackMapper.toUploadResponse(result.track(), result.job()));
    }

    @PostMapping(value = "/imports", consumes = "multipart/form-data")
    public ImportResponse importTrack(
            @RequestPart("file") MultipartFile file,
            @RequestParam(defaultValue = "HASH_TUNE") TrackOrigin origin,
            @RequestParam(required = false) String relativePath) {
        TrackService.ImportResult result = service.importTrack(file, origin, relativePath);
        return TrackMapper.toImportResponse(result.track(), result);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/metadata")
    public TrackResponse updateMetadata(@PathVariable UUID id, @RequestBody UpdateTrackMetadataRequest request) {
        return TrackMapper.toResponse(service.updateMetadata(id, request.title(), request.artist(), request.album()));
    }

    @PostMapping("/{id}/reindex")
    public ResponseEntity<IndexingJobResponse> reindex(@PathVariable UUID id) {
        return ResponseEntity.accepted().body(IndexingJobMapper.toResponse(service.reindex(id)));
    }

    @PostMapping("/reindex")
    public ResponseEntity<ReindexAllResponse> reindexAll() {
        TrackService.ReindexAllResult result = service.reindexAll();
        return ResponseEntity.accepted().body(LibraryMapper.reindexAll(result));
    }
}
