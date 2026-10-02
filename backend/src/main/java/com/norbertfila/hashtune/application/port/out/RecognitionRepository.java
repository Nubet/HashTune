package com.norbertfila.hashtune.application.port.out;

import com.norbertfila.hashtune.domain.recognition.Recognition;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RecognitionRepository {
    Recognition save(Recognition recognition);

    List<Recognition> findLatest(int limit, int offset);

    List<Recognition> findAll();

    Optional<Recognition> findById(UUID id);

    void deleteAll();
}
