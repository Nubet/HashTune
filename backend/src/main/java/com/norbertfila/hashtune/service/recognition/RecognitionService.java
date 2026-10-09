package com.norbertfila.hashtune.service.recognition;

import com.norbertfila.hashtune.configuration.StorageProperties;
import com.norbertfila.hashtune.entity.identity.ExternalIdentity;
import com.norbertfila.hashtune.entity.recognition.Recognition;
import com.norbertfila.hashtune.entity.recognition.RecognitionSource;
import com.norbertfila.hashtune.entity.recognition.RecognitionStatus;
import com.norbertfila.hashtune.entity.track.Track;
import com.norbertfila.hashtune.exceptions.application.ApplicationException;
import com.norbertfila.hashtune.exceptions.application.TooManyRequestsException;
import com.norbertfila.hashtune.repository.recognition.RecognitionRepository;
import com.norbertfila.hashtune.repository.track.TrackRepository;
import com.norbertfila.hashtune.security.RecognitionConcurrencyLimiter;
import com.norbertfila.hashtune.service.fingerprint.AudioRecognitionEngine;
import com.norbertfila.hashtune.service.storage.ObjectStoragePort;
import com.norbertfila.hashtune.service.validation.AudioUploadValidator;
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
public class RecognitionService {
    private final RecognitionRepository recognitions;
    private final RecognitionConcurrencyLimiter concurrencyLimiter;
    private final TrackRepository tracks;
    private final ObjectStoragePort storage;
    private final AudioRecognitionEngine engine;
    private final StorageProperties storageProperties;
    private final AudioUploadValidator audioUploadValidator;

    @Transactional
    public Recognition recognize(ExternalIdentity owner, MultipartFile file, RecognitionSource source) {
        return process(owner, file, source, true);
    }

    @Transactional
    public Recognition probe(ExternalIdentity owner, MultipartFile file, RecognitionSource source) {
        return process(owner, file, source, false);
    }

    private Recognition process(ExternalIdentity owner, MultipartFile file, RecognitionSource source, boolean persist) {
        if (!concurrencyLimiter.tryAcquire(owner)) {
            throw new TooManyRequestsException(java.time.Duration.ofSeconds(1));
        }
        try {
            return processAudio(owner, file, source, persist);
        } finally {
            concurrencyLimiter.release(owner);
        }
    }

    private Recognition processAudio(
            ExternalIdentity owner, MultipartFile file, RecognitionSource source, boolean persist) {
        audioUploadValidator.validate(file);
        UUID recognitionId = UUID.randomUUID();
        String key = "samples/" + recognitionId + "/" + safeName(file.getOriginalFilename());
        String recordingKey = "microphone-recordings/" + recognitionId + "/" + safeName(file.getOriginalFilename());
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
                    owner,
                    track == null ? null : track.id(),
                    result.matched() && track != null ? RecognitionStatus.MATCHED : RecognitionStatus.NO_MATCH,
                    result.matched() && track != null ? result.confidence() : null,
                    result.matched() && track != null ? result.matchedAtMs() : null,
                    source,
                    result.sampleDurationMs(),
                    System.currentTimeMillis() - started,
                    source == RecognitionSource.MICROPHONE && (persist || result.matched()) ? recordingKey : null,
                    source == RecognitionSource.MICROPHONE && (persist || result.matched())
                            ? file.getContentType()
                            : null,
                    source == RecognitionSource.MICROPHONE && (persist || result.matched())
                            ? safeName(file.getOriginalFilename())
                            : null,
                    Instant.now());
            boolean shouldPersist = persist || entity.status() == RecognitionStatus.MATCHED;
            if (!shouldPersist) return entity;
            if (entity.recordingObjectKey() != null) {
                try (InputStream recording = storage.get(storageProperties.getTempBucket(), key)) {
                    storage.put(
                            storageProperties.getAudioBucket(),
                            entity.recordingObjectKey(),
                            recording,
                            file.getSize(),
                            entity.recordingContentType());
                }
            }
            return recognitions.save(entity);
        } catch (IOException exception) {
            throw new ApplicationException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "INVALID_AUDIO", "Could not read audio sample");
        } finally {
            cleanupSample(key);
        }
    }

    public List<Recognition> history(ExternalIdentity owner, int limit, int offset) {
        return recognitions.findLatest(owner, Math.min(limit, 100), Math.max(offset, 0));
    }

    public Track track(UUID id) {
        return tracks.findById(id).orElse(null);
    }

    public Recording recording(ExternalIdentity owner, UUID id) {
        Recognition recognition = recognitions
                .findById(owner, id)
                .orElseThrow(() -> new ApplicationException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "RECORDING_NOT_FOUND", "Recording not found"));
        if (recognition.recordingObjectKey() == null) {
            throw new ApplicationException(
                    org.springframework.http.HttpStatus.NOT_FOUND, "RECORDING_NOT_FOUND", "Recording not found");
        }
        return new Recording(
                storage.get(storageProperties.getAudioBucket(), recognition.recordingObjectKey()),
                recognition.recordingContentType() == null
                        ? "application/octet-stream"
                        : recognition.recordingContentType(),
                recognition.recordingFileName() == null ? "microphone-recording" : recognition.recordingFileName());
    }

    @Transactional
    public void clearHistory(ExternalIdentity owner) {
        recognitions.findAll(owner).stream()
                .map(Recognition::recordingObjectKey)
                .filter(java.util.Objects::nonNull)
                .forEach(this::deleteRecording);
        recognitions.deleteAll(owner);
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

    private void deleteRecording(String key) {
        try {
            storage.delete(storageProperties.getAudioBucket(), key);
        } catch (RuntimeException cleanupFailure) {
            log.warn("Could not clean up microphone recording {}", key, cleanupFailure);
        }
    }

    public record Recording(InputStream content, String contentType, String fileName) {}
}
