package com.norbertfila.hashtune.repository.fingerprint;

import com.norbertfila.hashtune.entity.fingerprint.FingerprintEntity;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataFingerprintRepository extends JpaRepository<FingerprintEntity, Long> {
    List<FingerprintEntity> findByHashIn(Collection<Long> hashes);

    long countByTrackId(UUID trackId);

    void deleteByTrackId(UUID trackId);
}
