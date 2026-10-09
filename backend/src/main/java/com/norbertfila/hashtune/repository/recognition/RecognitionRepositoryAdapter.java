package com.norbertfila.hashtune.repository.recognition;

import com.norbertfila.hashtune.entity.recognition.RecognitionEntity;
import com.norbertfila.hashtune.repository.recognition.RecognitionRepository;
import com.norbertfila.hashtune.entity.identity.ExternalIdentity;
import com.norbertfila.hashtune.entity.recognition.Recognition;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RecognitionRepositoryAdapter implements RecognitionRepository {
    private final SpringDataRecognitionRepository repository;

    @Override
    public Recognition save(Recognition recognition) {
        return toDomain(repository.save(toEntity(recognition)));
    }

    @Override
    public List<Recognition> findLatest(ExternalIdentity owner, int limit, int offset) {
        return repository
                .findByOwnerIssuerAndOwnerSubject(
                        owner.issuer(),
                        owner.subject(),
                        PageRequest.of(
                                offset / limit,
                                limit,
                                Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"))))
                .getContent()
                .stream()
                .map(RecognitionRepositoryAdapter::toDomain)
                .toList();
    }

    @Override
    public List<Recognition> findAll(ExternalIdentity owner) {
        return repository
                .findAllByOwnerIssuerAndOwnerSubjectOrderByCreatedAtDesc(owner.issuer(), owner.subject())
                .stream()
                .map(RecognitionRepositoryAdapter::toDomain)
                .toList();
    }

    @Override
    public Optional<Recognition> findById(ExternalIdentity owner, UUID id) {
        return repository
                .findByIdAndOwnerIssuerAndOwnerSubject(id, owner.issuer(), owner.subject())
                .map(RecognitionRepositoryAdapter::toDomain);
    }

    @Override
    public void deleteAll(ExternalIdentity owner) {
        repository.deleteAllByOwnerIssuerAndOwnerSubject(owner.issuer(), owner.subject());
    }

    private static RecognitionEntity toEntity(Recognition recognition) {
        return RecognitionEntity.builder()
                .id(recognition.id())
                .ownerIssuer(recognition.owner().issuer())
                .ownerSubject(recognition.owner().subject())
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
                new ExternalIdentity(recognition.getOwnerIssuer(), recognition.getOwnerSubject()),
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
