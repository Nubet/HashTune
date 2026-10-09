package com.norbertfila.hashtune.mapper;

import com.norbertfila.hashtune.dto.response.IndexingJobResponse;
import com.norbertfila.hashtune.entity.indexing.IndexingJob;

public final class IndexingJobMapper {
    private IndexingJobMapper() {}

    public static IndexingJobResponse toResponse(IndexingJob job) {
        return new IndexingJobResponse(
                job.id(),
                job.trackId(),
                job.status().name(),
                job.progress(),
                job.attempts(),
                job.nextAttemptAt(),
                job.errorCode(),
                job.errorMessage());
    }
}
