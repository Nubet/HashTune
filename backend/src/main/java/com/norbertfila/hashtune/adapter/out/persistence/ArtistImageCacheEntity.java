package com.norbertfila.hashtune.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "artist_image_cache")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArtistImageCacheEntity {
    @Id
    @Column(name = "artist_key", length = 255)
    private String artistKey;

    @Column(name = "display_name", nullable = false, length = 255)
    private String displayName;

    @Column(name = "image_url", length = 2048)
    private String imageUrl;

    @Column(name = "checked_at", nullable = false)
    private Instant checkedAt;
}
