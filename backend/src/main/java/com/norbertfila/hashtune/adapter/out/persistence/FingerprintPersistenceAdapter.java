package com.norbertfila.hashtune.adapter.out.persistence;

import com.norbertfila.hashtune.entity.fingerprint.FingerprintEntity;
import com.norbertfila.hashtune.application.port.out.FingerprintRepository;
import com.norbertfila.hashtune.entity.fingerprint.FingerprintOccurrence;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class FingerprintPersistenceAdapter implements FingerprintRepository {
    private static final int MAX_HASHES_PER_QUERY = 10_000;
    private static final int INSERT_BATCH_SIZE = 1_000;

    private final SpringDataFingerprintRepository repository;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void replace(UUID trackId, List<FingerprintOccurrence> fingerprints) {
        repository.deleteByTrackId(trackId);
        jdbcTemplate.batchUpdate(
                "INSERT INTO audio_fingerprints (hash, track_id, anchor_offset_ms) VALUES (?, ?, ?)",
                fingerprints,
                INSERT_BATCH_SIZE,
                (statement, fingerprint) -> {
                    statement.setLong(1, fingerprint.hash());
                    statement.setObject(2, trackId);
                    statement.setInt(3, fingerprint.anchorOffsetMs());
                });
    }

    @Override
    @Transactional(readOnly = true)
    public long countByTrackId(UUID trackId) {
        return repository.countByTrackId(trackId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FingerprintMatch> findMatches(Collection<Long> hashes) {
        if (hashes.isEmpty()) {
            return List.of();
        }
        List<Long> distinctHashes = hashes.stream().distinct().toList();
        List<FingerprintMatch> matches = new ArrayList<>();
        for (int offset = 0; offset < distinctHashes.size(); offset += MAX_HASHES_PER_QUERY) {
            int end = Math.min(offset + MAX_HASHES_PER_QUERY, distinctHashes.size());
            repository.findByHashIn(distinctHashes.subList(offset, end)).stream()
                    .map(fingerprint -> new FingerprintMatch(
                            fingerprint.getHash(), fingerprint.getTrackId(), fingerprint.getAnchorOffsetMs()))
                    .forEach(matches::add);
        }
        return matches;
    }

    @Override
    @Transactional
    public void deleteByTrackId(UUID trackId) {
        repository.deleteByTrackId(trackId);
    }
}
