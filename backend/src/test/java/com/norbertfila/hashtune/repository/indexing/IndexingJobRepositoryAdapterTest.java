package com.norbertfila.hashtune.repository.indexing;

import com.norbertfila.hashtune.entity.indexing.IndexingJobEntity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.norbertfila.hashtune.entity.indexing.IndexingJob;
import com.norbertfila.hashtune.entity.indexing.IndexingJobStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

class IndexingJobRepositoryAdapterTest {
    private final SpringDataJobRepository repository = mock(SpringDataJobRepository.class);
    private final IndexingJobRepositoryAdapter adapter = new IndexingJobRepositoryAdapter(repository);

    @Test
    void claimsOnlyJobsReadyForAnotherAttempt() {
        IndexingJobEntity entity = IndexingJobEntity.builder()
                .id(UUID.randomUUID())
                .trackId(UUID.randomUUID())
                .status(IndexingJobStatus.PENDING)
                .progress(0)
                .attempts(1)
                .createdAt(Instant.now())
                .build();
        when(repository.findReadyJobs(eq(IndexingJobStatus.PENDING), any(), any(Pageable.class)))
                .thenReturn(List.of(entity));
        when(repository.save(entity)).thenReturn(entity);

        Optional<IndexingJob> claimed = adapter.claimNextPending(Instant.now());

        assertThat(claimed).isPresent().get().extracting(IndexingJob::status).isEqualTo(IndexingJobStatus.PROCESSING);
        assertThat(entity.getAttempts()).isEqualTo(2);
        verify(repository).findReadyJobs(eq(IndexingJobStatus.PENDING), any(), any(Pageable.class));
    }

    @Test
    void recoversStaleProcessingJobsAsPending() {
        when(repository.recoverStaleProcessing(
                        eq(IndexingJobStatus.PROCESSING),
                        eq(IndexingJobStatus.PENDING),
                        any(),
                        any(),
                        eq("INDEXING_RECOVERED"),
                        any()))
                .thenReturn(2);

        int recovered = adapter.recoverStaleProcessing(Instant.now().minusSeconds(60), Instant.now());

        assertThat(recovered).isEqualTo(2);
        verify(repository)
                .recoverStaleProcessing(
                        eq(IndexingJobStatus.PROCESSING),
                        eq(IndexingJobStatus.PENDING),
                        any(),
                        any(),
                        eq("INDEXING_RECOVERED"),
                        eq("Recovered after the indexing worker timed out"));
    }
}
