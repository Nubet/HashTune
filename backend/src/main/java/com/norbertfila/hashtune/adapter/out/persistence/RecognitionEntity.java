package com.norbertfila.hashtune.adapter.out.persistence;

import com.norbertfila.hashtune.domain.recognition.RecognitionSource;
import com.norbertfila.hashtune.domain.recognition.RecognitionStatus;
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
@Table(name = "recognitions")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecognitionEntity {
    @Id
    private UUID id;

    private UUID trackId;

    @Enumerated(EnumType.STRING)
    private RecognitionStatus status;

    private Double confidence;
    private Long matchedAtMs;

    @Enumerated(EnumType.STRING)
    private RecognitionSource source;

    private Long sampleDurationMs;
    private Long recognitionTimeMs;
    private Instant createdAt;
}
