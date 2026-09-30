package com.norbertfila.hashtune.application.service;

import com.norbertfila.hashtune.application.port.out.AudioRecognitionEngine;
import com.norbertfila.hashtune.application.port.out.ObjectStoragePort;
import com.norbertfila.hashtune.application.port.out.RecognitionRepository;
import com.norbertfila.hashtune.application.port.out.TrackRepository;
import com.norbertfila.hashtune.configuration.AudioProperties;
import com.norbertfila.hashtune.configuration.StorageProperties;
import com.norbertfila.hashtune.domain.recognition.Recognition;
import com.norbertfila.hashtune.domain.recognition.RecognitionSource;
import com.norbertfila.hashtune.domain.recognition.RecognitionStatus;
import com.norbertfila.hashtune.domain.track.Track;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecognitionApplicationService {
    private final RecognitionRepository recognitions;
    private final TrackRepository tracks;
    private final ObjectStoragePort storage;
    private final AudioRecognitionEngine engine;
    private final StorageProperties storageProperties;
    private final AudioProperties audioProperties;

    @Transactional
    public Recognition recognize(MultipartFile file, RecognitionSource source) {
        return process(file, source, true);
    }

    public Recognition probe(MultipartFile file, RecognitionSource source) {
        return process(file, source, false);
    }

    private Recognition process(MultipartFile file, RecognitionSource source, boolean persist) {
        if (file == null || file.isEmpty() || file.getSize() > audioProperties.getMaxFileSizeBytes()) {
            throw new ApplicationException(
                    org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY,
                    "INVALID_AUDIO",
                    "Audio sample is empty or too large");
        }
        UUID recognitionId = UUID.randomUUID();
        String key = "samples/" + recognitionId + "/" + safeName(file.getOriginalFilename());
        long started = System.currentTimeMillis();
        try {
            try (InputStream input = file.getInputStream()) {
                storage.put(storageProperties.getTempBucket(), key, input, file.getSize(), file.getContentType());
            }
            AudioRecognitionEngine.RecognitionResult result = engine.recognize(new AudioRecognitionEngine.InputAudio(
                    storageProperties.getTempBucket(), key, file.getOriginalFilename(), ""));
            Track track = result.matched() && result.trackId() != null
                    ? tracks.findById(result.trackId()).orElse(null)
                    : null;
            Recognition entity = new Recognition(
                    recognitionId,
                    track == null ? null : track.id(),
                    result.matched() && track != null ? RecognitionStatus.MATCHED : RecognitionStatus.NO_MATCH,
                    result.matched() && track != null ? result.confidence() : null,
                    result.matched() && track != null ? result.matchedAtMs() : null,
                    source,
                    result.sampleDurationMs(),
                    System.currentTimeMillis() - started,
                    Instant.now());
            return persist ? recognitions.save(entity) : entity;
        } catch (IOException exception) {
            throw new ApplicationException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "INVALID_AUDIO", "Could not read audio sample");
        } finally {
            cleanupSample(key);
        }
    }

    public List<Recognition> history(int limit, int offset) {
        return recognitions.findLatest(Math.min(limit, 100), Math.max(offset, 0));
    }

    public Track track(UUID id) {
        return tracks.findById(id).orElse(null);
    }

    @Transactional
    public void clearHistory() {
        recognitions.deleteAll();
    }

    private String safeName(String name) {
        return name == null ? "sample" : name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private void cleanupSample(String key) {
        try {
            storage.delete(storageProperties.getTempBucket(), key);
        } catch (RuntimeException cleanupFailure) {
            log.warn("Could not clean up recognition sample {}", key, cleanupFailure);
        }
    }
}
