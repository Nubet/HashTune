package com.norbertfila.hashtune.adapter.out.fingerprinting;

import com.norbertfila.hashtune.application.port.out.AudioRecognitionEngine;
import com.norbertfila.hashtune.application.port.out.ObjectStoragePort;
import com.norbertfila.hashtune.configuration.AudioProperties;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.audio.engine", havingValue = "spectral")
public class SpectralAudioRecognitionEngine implements AudioRecognitionEngine {
    private static final long MILLISECONDS_PER_SECOND = 1_000;

    private final ObjectStoragePort storage;
    private final FfmpegAudioDecoder decoder;
    private final SpectralFingerprinting fingerprinting;

    public SpectralAudioRecognitionEngine(ObjectStoragePort storage, AudioProperties audioProperties) {
        this.storage = storage;
        this.decoder = new FfmpegAudioDecoder(audioProperties.getFfmpegBinary());
        this.fingerprinting = new SpectralFingerprinting();
    }

    @Override
    public IndexingResult index(UUID trackId, InputAudio audio) {
        DecodedAudio decodedAudio = decode(audio);
        List<FingerprintOccurrence> fingerprints =
                fingerprinting.fingerprint(new AudioSamples(decodedAudio.samples(), decodedAudio.sampleRate()));
        return new IndexingResult(
                Math.round(decodedAudio.durationSeconds() * MILLISECONDS_PER_SECOND), fingerprints.size());
    }

    @Override
    public RecognitionResult recognize(InputAudio audio) {
        DecodedAudio decodedAudio = decode(audio);
        return RecognitionResult.noMatch(Math.round(decodedAudio.durationSeconds() * MILLISECONDS_PER_SECOND));
    }

    private DecodedAudio decode(InputAudio audio) {
        try (InputStream content = storage.get(audio.bucket(), audio.objectKey())) {
            return decoder.decode(content);
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Could not close audio input", exception);
        }
    }
}
