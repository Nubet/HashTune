package com.norbertfila.hashtune.adapter.in.web;

import com.norbertfila.hashtune.application.service.RecognitionApplicationService;
import com.norbertfila.hashtune.domain.recognition.RecognitionSource;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class RecognitionController {
    private final RecognitionApplicationService service;

    @PostMapping(value = "/recognitions", consumes = "multipart/form-data")
    public ApiDtos.RecognitionResponse recognize(
            @RequestPart("file") MultipartFile file,
            @RequestParam(defaultValue = "AUDIO_FILE") RecognitionSource source,
            @RequestParam(defaultValue = "false") boolean probe) {
        var result = probe ? service.probe(file, source) : service.recognize(file, source);
        var track = result.trackId() == null ? null : service.track(result.trackId());
        return ApiDtos.RecognitionResponse.from(result, track);
    }

    @GetMapping("/recognition-history")
    public List<ApiDtos.HistoryResponse> history(
            @RequestParam(defaultValue = "25") int limit, @RequestParam(defaultValue = "0") int offset) {
        return service.history(limit, offset).stream()
                .map(item -> ApiDtos.HistoryResponse.from(
                        item, item.trackId() == null ? null : service.track(item.trackId())))
                .toList();
    }

    @DeleteMapping("/recognition-history")
    public ResponseEntity<Void> clearHistory() {
        service.clearHistory();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/recognition-history/{id}/recording")
    public ResponseEntity<InputStreamResource> recording(
            @PathVariable java.util.UUID id, @RequestParam(defaultValue = "false") boolean download) {
        var recording = service.recording(id);
        return ResponseEntity.ok()
                .contentType(contentType(recording.contentType()))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        (download ? "attachment" : "inline") + "; filename=\"" + recording.fileName() + "\"")
                .body(new InputStreamResource(recording.content()));
    }

    private MediaType contentType(String value) {
        try {
            return MediaType.parseMediaType(value);
        } catch (IllegalArgumentException ignored) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
