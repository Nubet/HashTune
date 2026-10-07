package com.norbertfila.hashtune.adapter.out.persistence;

import com.norbertfila.hashtune.application.port.out.RecognitionRepository;
import com.norbertfila.hashtune.domain.recognition.Recognition;
import com.norbertfila.hashtune.domain.session.ClientSessionId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RecognitionPersistenceAdapter implements RecognitionRepository {
    private final SpringDataRecognitionRepository repository;

    @Override
    public Recognition save(Recognition recognition) {
        return toDomain(repository.save(toEntity(recognition)));
    }

    @Override
    public List<Recognition> findLatest(ClientSessionId sessionId, int limit, int offset) {
        return repository
                .findBySessionId(
                        sessionId.value(),
                        PageRequest.of(
                                offset / limit,
                                limit,
                                Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"))))
                .getContent()
                .stream()
                .map(RecognitionPersistenceAdapter::toDomain)
                .toList();
    }

    @Override
    public List<Recognition> findAll(ClientSessionId sessionId) {
        return repository.findAllBySessionIdOrderByCreatedAtDesc(sessionId.value()).stream()
                .map(RecognitionPersistenceAdapter::toDomain)
                .toList();
    }

    @Override
    public Optional<Recognition> findById(ClientSessionId sessionId, UUID id) {
        return repository.findByIdAndSessionId(id, sessionId.value()).map(RecognitionPersistenceAdapter::toDomain);
    }

    @Override
    public void deleteAll(ClientSessionId sessionId) {
        repository.deleteAllBySessionId(sessionId.value());
    }

    private static RecognitionEntity toEntity(Recognition recognition) {
        return RecognitionEntity.builder()
                .id(recognition.id())
                .sessionId(recognition.sessionId().value())
                .trackId(recognition.trackId())
                .status(recognition.status())
                .confidence(recognition.confidence())
                .matchedAtMs(recognition.matchedAtMs())
                .source(recognition.source())
                .sampleDurationMs(recognition.sampleDurationMs())
                .recognitionTimeMs(recognition.recognitionTimeMs())
                .recordingObjectKey(recognition.recordingObjectKey())
                .recordingContentType(recognition.recordingContentType())
                .recordingFileName(recognition.recordingFileName())
                .createdAt(recognition.createdAt())
                .build();
    }

    private static Recognition toDomain(RecognitionEntity recognition) {
        return new Recognition(
                recognition.getId(),
                new ClientSessionId(recognition.getSessionId()),
                recognition.getTrackId(),
                recognition.getStatus(),
                recognition.getConfidence(),
                recognition.getMatchedAtMs(),
                recognition.getSource(),
                recognition.getSampleDurationMs(),
                recognition.getRecognitionTimeMs(),
                recognition.getRecordingObjectKey(),
                recognition.getRecordingContentType(),
                recognition.getRecordingFileName(),
                recognition.getCreatedAt());
    }
}
