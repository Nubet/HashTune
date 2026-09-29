package com.norbertfila.hashtune.adapter.out.persistence;

import com.norbertfila.hashtune.application.port.out.IndexingJobRepository;
import com.norbertfila.hashtune.domain.indexing.IndexingJob;
import com.norbertfila.hashtune.domain.indexing.IndexingJobStatus;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class JobPersistenceAdapter implements IndexingJobRepository {
    private final SpringDataJobRepository repository;

    @Override
    public IndexingJob save(IndexingJob job) {
        return toDomain(repository.save(toEntity(job)));
    }

    @Override
    public Optional<IndexingJob> findById(UUID id) {
        return repository.findById(id).map(JobPersistenceAdapter::toDomain);
    }

    @Override
    public boolean existsByTrackId(UUID trackId) {
        return repository.existsByTrackId(trackId);
    }

    @Override
    @Transactional
    public Optional<IndexingJob> claimNextPending() {
        return repository.findByStatusOrderByCreatedAtAsc(IndexingJobStatus.PENDING).stream()
                .findFirst()
                .map(job -> {
                    job.start();
                    return toDomain(repository.save(job));
                });
    }

    private static IndexingJobEntity toEntity(IndexingJob job) {
        return IndexingJobEntity.builder()
                .id(job.id())
                .trackId(job.trackId())
                .status(job.status())
                .progress(job.progress())
                .attempts(job.attempts())
                .errorCode(job.errorCode())
                .errorMessage(job.errorMessage())
                .createdAt(job.createdAt())
                .startedAt(job.startedAt())
                .finishedAt(job.finishedAt())
                .build();
    }

    private static IndexingJob toDomain(IndexingJobEntity job) {
        return new IndexingJob(
                job.getId(),
                job.getTrackId(),
                job.getStatus(),
                job.getProgress(),
                job.getAttempts(),
                job.getErrorCode(),
                job.getErrorMessage(),
                job.getCreatedAt(),
                job.getStartedAt(),
                job.getFinishedAt());
    }
}
