package com.norbertfila.hashtune.repository.artist;

import com.norbertfila.hashtune.entity.health.ArtistImageCacheEntity;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataArtistImageCacheRepository extends JpaRepository<ArtistImageCacheEntity, String> {}
