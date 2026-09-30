package com.norbertfila.hashtune.adapter.out.persistence;

import com.norbertfila.hashtune.application.port.out.RecognitionRepository;
import com.norbertfila.hashtune.domain.recognition.Recognition;
import java.util.List;
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
    public List<Recognition> findLatest(int limit, int offset) {
        return repository
                .findAll(PageRequest.of(
                        offset / limit,
                        limit,
                        Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"))))
                .getContent()
                .stream()
                .map(RecognitionPersistenceAdapter::toDomain)
                .toList();
    }

    @Override
    public void deleteAll() {
        repository.deleteAllInBatch();
    }

    private static RecognitionEntity toEntity(Recognition recognition) {
        return RecognitionEntity.builder()
                .id(recognition.id())
                .trackId(recognition.trackId())
                .status(recognition.status())
                .confidence(recognition.confidence())
                .matchedAtMs(recognition.matchedAtMs())
                .source(recognition.source())
                .sampleDurationMs(recognition.sampleDurationMs())
                .recognitionTimeMs(recognition.recognitionTimeMs())
                .createdAt(recognition.createdAt())
                .build();
    }

    private static Recognition toDomain(RecognitionEntity recognition) {
        return new Recognition(
                recognition.getId(),
                recognition.getTrackId(),
                recognition.getStatus(),
                recognition.getConfidence(),
                recognition.getMatchedAtMs(),
                recognition.getSource(),
                recognition.getSampleDurationMs(),
                recognition.getRecognitionTimeMs(),
                recognition.getCreatedAt());
    }
}
