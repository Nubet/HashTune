package com.norbertfila.hashtune.adapter.out.fingerprinting;

import com.norbertfila.hashtune.application.port.out.AudioRecognitionEngine;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.audio.engine", havingValue = "mock", matchIfMissing = true)
public class MockAudioRecognitionEngine implements AudioRecognitionEngine {
    @Override
    public IndexingResult index(UUID trackId, InputAudio audio) {
        return new IndexingResult(180_000, 18_240);
    }

    @Override
    public RecognitionResult recognize(InputAudio audio) {
        if (audio.fileName() != null && audio.fileName().toLowerCase().contains("no-match")) {
            return RecognitionResult.noMatch(5_000);
        }
        return new RecognitionResult(true, null, 0.96, 137_000, 5_000, 302, 287);
    }
}
