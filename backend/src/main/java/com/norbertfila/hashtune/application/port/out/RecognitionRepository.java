package com.norbertfila.hashtune.application.port.out;

import com.norbertfila.hashtune.domain.recognition.Recognition;
import com.norbertfila.hashtune.domain.session.ClientSessionId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RecognitionRepository {
    Recognition save(Recognition recognition);

    List<Recognition> findLatest(ClientSessionId sessionId, int limit, int offset);

    List<Recognition> findAll(ClientSessionId sessionId);

    Optional<Recognition> findById(ClientSessionId sessionId, UUID id);

    void deleteAll(ClientSessionId sessionId);
}
