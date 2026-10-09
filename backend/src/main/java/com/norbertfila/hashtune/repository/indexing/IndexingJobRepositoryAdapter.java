package com.norbertfila.hashtune.repository.indexing;

import com.norbertfila.hashtune.entity.indexing.IndexingJob;
import com.norbertfila.hashtune.entity.indexing.IndexingJobEntity;
import com.norbertfila.hashtune.entity.indexing.IndexingJobStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class IndexingJobRepositoryAdapter implements IndexingJobRepository {
    private final SpringDataJobRepository repository;

    @Override
    public IndexingJob save(IndexingJob job) {
        return toDomain(repository.save(toEntity(job)));
    }

    @Override
    public Optional<IndexingJob> findById(UUID id) {
        return repository.findById(id).map(IndexingJobRepositoryAdapter::toDomain);
    }

    @Override
    public Optional<IndexingJob> findByTrackId(UUID trackId) {
        return repository.findByTrackId(trackId).map(IndexingJobRepositoryAdapter::toDomain);
    }

    @Override
    public boolean existsByTrackId(UUID trackId) {
        return repository.existsByTrackId(trackId);
    }

    @Override
    public boolean existsByTrackIdAndStatusIn(UUID trackId, List<IndexingJobStatus> statuses) {
        return repository.existsByTrackIdAndStatusIn(trackId, statuses);
    }

    @Override
    @Transactional
    public Optional<IndexingJob> claimNextPending(Instant now) {
        return repository.findReadyJobs(IndexingJobStatus.PENDING, now, PageRequest.of(0, 1)).stream()
                .findFirst()
                .map(job -> {
                    job.start();
                    return toDomain(repository.save(job));
                });
    }

    @Override
    @Transactional
    public int recoverStaleProcessing(Instant cutoff, Instant nextAttemptAt) {
        return repository.recoverStaleProcessing(
                IndexingJobStatus.PROCESSING,
                IndexingJobStatus.PENDING,
                cutoff,
                nextAttemptAt,
                "INDEXING_RECOVERED",
                "Recovered after the indexing worker timed out");
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
                job.getFinishedAt(),
                job.getNextAttemptAt());
    }
}
