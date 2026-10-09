package com.norbertfila.hashtune.entity.recognition;

import com.norbertfila.hashtune.entity.recognition.RecognitionSource;
import com.norbertfila.hashtune.entity.recognition.RecognitionStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.Column;
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

    @Column(name = "owner_issuer", nullable = false)
    private String ownerIssuer;

    @Column(name = "owner_subject", nullable = false)
    private String ownerSubject;
    private UUID trackId;

    @Enumerated(EnumType.STRING)
    private RecognitionStatus status;

    private Double confidence;
    private Long matchedAtMs;

    @Enumerated(EnumType.STRING)
    private RecognitionSource source;

    private Long sampleDurationMs;
    private Long recognitionTimeMs;
    private String recordingObjectKey;
    private String recordingContentType;
    private String recordingFileName;
    private Instant createdAt;
}
