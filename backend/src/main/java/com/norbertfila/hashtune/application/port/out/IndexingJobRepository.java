package com.norbertfila.hashtune.application.port.out;

import com.norbertfila.hashtune.domain.indexing.IndexingJob;
import com.norbertfila.hashtune.domain.indexing.IndexingJobStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IndexingJobRepository {
    IndexingJob save(IndexingJob job);

    Optional<IndexingJob> findById(UUID id);

    Optional<IndexingJob> findByTrackId(UUID trackId);

    boolean existsByTrackId(UUID trackId);

    boolean existsByTrackIdAndStatusIn(UUID trackId, List<IndexingJobStatus> statuses);

    Optional<IndexingJob> claimNextPending();
}
