package com.norbertfila.hashtune.adapter.out.persistence;

import com.norbertfila.hashtune.application.port.out.FingerprintRepository;
import com.norbertfila.hashtune.domain.fingerprint.FingerprintOccurrence;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class FingerprintPersistenceAdapter implements FingerprintRepository {
    private final SpringDataFingerprintRepository repository;

    @Override
    @Transactional
    public void replace(UUID trackId, List<FingerprintOccurrence> fingerprints) {
        repository.deleteByTrackId(trackId);
        repository.saveAll(fingerprints.stream()
                .map(fingerprint -> FingerprintEntity.builder()
                        .id(UUID.randomUUID())
                        .hash(fingerprint.hash())
                        .trackId(trackId)
                        .anchorOffsetMs(fingerprint.anchorOffsetMs())
                        .build())
                .toList());
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
        return repository.findByHashIn(hashes).stream()
                .map(fingerprint -> new FingerprintMatch(
                        fingerprint.getHash(), fingerprint.getTrackId(), fingerprint.getAnchorOffsetMs()))
                .toList();
    }

    @Override
    @Transactional
    public void deleteByTrackId(UUID trackId) {
        repository.deleteByTrackId(trackId);
    }
}
