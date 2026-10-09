package com.norbertfila.hashtune.repository.recognition;

import com.norbertfila.hashtune.entity.identity.ExternalIdentity;
import com.norbertfila.hashtune.entity.recognition.Recognition;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RecognitionRepository {
    Recognition save(Recognition recognition);

    List<Recognition> findLatest(ExternalIdentity owner, int limit, int offset);

    List<Recognition> findAll(ExternalIdentity owner);

    Optional<Recognition> findById(ExternalIdentity owner, UUID id);

    void deleteAll(ExternalIdentity owner);
}
