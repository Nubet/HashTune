package com.norbertfila.hashtune.adapter.out.persistence;

import com.norbertfila.hashtune.domain.indexing.IndexingJobStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

interface SpringDataJobRepository extends JpaRepository<IndexingJobEntity, UUID> {
    boolean existsByTrackId(UUID trackId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<IndexingJobEntity> findByStatusOrderByCreatedAtAsc(IndexingJobStatus status);
}
