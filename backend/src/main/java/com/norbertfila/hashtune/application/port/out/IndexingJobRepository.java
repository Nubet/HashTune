package com.norbertfila.hashtune.application.port.out;

import com.norbertfila.hashtune.domain.indexing.IndexingJob;
import java.util.Optional;
import java.util.UUID;

public interface IndexingJobRepository {
    IndexingJob save(IndexingJob job);

    Optional<IndexingJob> findById(UUID id);

    Optional<IndexingJob> claimNextPending();
}
