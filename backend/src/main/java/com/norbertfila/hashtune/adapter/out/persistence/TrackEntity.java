package com.norbertfila.hashtune.adapter.out.persistence;

import com.norbertfila.hashtune.domain.track.TrackStatus;
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
@Table(name = "tracks")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrackEntity {
    @Id
    private UUID id;

    private String title;
    private String artist;
    private String album;
    private Long durationMs;
    private String audioObjectKey;
    private String checksum;

    @Enumerated(EnumType.STRING)
    private TrackStatus status;

    private Instant createdAt;
    private Instant updatedAt;
}
