package com.norbertfila.hashtune.adapter.out.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "audio_fingerprints",
        indexes = {
            @Index(name = "idx_audio_fingerprints_hash", columnList = "hash"),
            @Index(name = "idx_audio_fingerprints_track_id", columnList = "trackId")
        })
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FingerprintEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private long hash;
    private UUID trackId;
    private int anchorOffsetMs;
}
