package com.norbertfila.hashtune.adapter.out.persistence;

import com.norbertfila.hashtune.domain.indexing.IndexingJobStatus;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface SpringDataJobRepository extends JpaRepository<IndexingJobEntity, UUID> {
    boolean existsByTrackId(UUID trackId);

    Optional<IndexingJobEntity> findByTrackId(UUID trackId);

    boolean existsByTrackIdAndStatusIn(UUID trackId, List<IndexingJobStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT job FROM IndexingJobEntity job
            WHERE job.status = :status
              AND (job.nextAttemptAt IS NULL OR job.nextAttemptAt <= :now)
            ORDER BY job.createdAt ASC
            """)
    List<IndexingJobEntity> findReadyJobs(@Param("status") IndexingJobStatus status, @Param("now") Instant now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE IndexingJobEntity job
            SET job.status = :pending,
                job.progress = 0,
                job.startedAt = NULL,
                job.finishedAt = NULL,
                job.nextAttemptAt = :nextAttemptAt,
                job.errorCode = :errorCode,
                job.errorMessage = :errorMessage
            WHERE job.status = :processing
              AND job.startedAt < :cutoff
            """)
    int recoverStaleProcessing(
            @Param("processing") IndexingJobStatus processing,
            @Param("pending") IndexingJobStatus pending,
            @Param("cutoff") Instant cutoff,
            @Param("nextAttemptAt") Instant nextAttemptAt,
            @Param("errorCode") String errorCode,
            @Param("errorMessage") String errorMessage);
}
