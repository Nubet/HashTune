package com.norbertfila.hashtune.adapter.in.web;

import com.norbertfila.hashtune.application.service.TrackApplicationService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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

    @GetMapping
    public List<ApiDtos.TrackResponse> search(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "25") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        return service.search(query, limit, offset).stream()
                .map(ApiDtos.TrackResponse::from)
                .toList();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reindex")
    public ResponseEntity<ApiDtos.IndexingJobResponse> reindex(@PathVariable UUID id) {
        return ResponseEntity.accepted().body(ApiDtos.IndexingJobResponse.from(service.reindex(id)));
    }
}
