package com.norbertfila.hashtune.application.port.out;

import com.norbertfila.hashtune.domain.recognition.Recognition;
import java.util.List;

public interface RecognitionRepository {
    Recognition save(Recognition recognition);

    List<Recognition> findLatest(int limit, int offset);

    void deleteAll();
}
