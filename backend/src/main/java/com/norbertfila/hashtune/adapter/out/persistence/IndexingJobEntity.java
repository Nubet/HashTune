package com.norbertfila.hashtune.adapter.out.persistence;

import com.norbertfila.hashtune.domain.indexing.IndexingJobStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "indexing_jobs")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IndexingJobEntity {
    @Id
    private UUID id;

    private UUID trackId;

    @Enumerated(EnumType.STRING)
    private IndexingJobStatus status;

    private int progress;
    private int attempts;
    private String errorCode;
    private String errorMessage;
    private Instant createdAt;
    private Instant startedAt;
    private Instant finishedAt;
    private Instant nextAttemptAt;

    public void start() {
        status = IndexingJobStatus.PROCESSING;
        progress = 10;
        attempts++;
        startedAt = Instant.now();
        finishedAt = null;
        nextAttemptAt = null;
        errorCode = null;
        errorMessage = null;
    }

    public void complete() {
        status = IndexingJobStatus.COMPLETED;
        progress = 100;
        finishedAt = Instant.now();
    }

    public void fail(String code, String message) {
        status = IndexingJobStatus.FAILED;
        errorCode = code;
        errorMessage = message;
        finishedAt = Instant.now();
    }
}
