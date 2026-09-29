package com.norbertfila.hashtune.adapter.out.fingerprinting;

import com.norbertfila.hashtune.application.port.out.AudioRecognitionEngine;
import com.norbertfila.hashtune.application.port.out.FingerprintRepository;
import com.norbertfila.hashtune.application.port.out.ObjectStoragePort;
import com.norbertfila.hashtune.configuration.AudioProperties;
import com.norbertfila.hashtune.domain.fingerprint.FingerprintOccurrence;
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
    private final FingerprintRepository fingerprints;
    private final FingerprintMatcher matcher;

    public SpectralAudioRecognitionEngine(
            ObjectStoragePort storage, AudioProperties audioProperties, FingerprintRepository fingerprints) {
        this.storage = storage;
        this.decoder = new FfmpegAudioDecoder(audioProperties.getFfmpegBinary());
        this.fingerprinting = new SpectralFingerprinting();
        this.fingerprints = fingerprints;
        this.matcher = new FingerprintMatcher();
    }

    @Override
    public IndexingResult index(UUID trackId, InputAudio audio) {
        DecodedAudio decodedAudio = decode(audio);
        List<FingerprintOccurrence> fingerprintOccurrences =
                fingerprinting.fingerprint(new AudioSamples(decodedAudio.samples(), decodedAudio.sampleRate()));
        fingerprints.replace(trackId, fingerprintOccurrences);
        return new IndexingResult(
                Math.round(decodedAudio.durationSeconds() * MILLISECONDS_PER_SECOND), fingerprintOccurrences.size());
    }

    @Override
    public RecognitionResult recognize(InputAudio audio) {
        DecodedAudio decodedAudio = decode(audio);
        List<FingerprintOccurrence> sampleFingerprints =
                fingerprinting.fingerprint(new AudioSamples(decodedAudio.samples(), decodedAudio.sampleRate()));
        FingerprintMatcher.MatchResult match = matcher.match(
                sampleFingerprints,
                fingerprints.findMatches(sampleFingerprints.stream()
                        .map(FingerprintOccurrence::hash)
                        .distinct()
                        .toList()));
        long sampleDurationMs = Math.round(decodedAudio.durationSeconds() * MILLISECONDS_PER_SECOND);
        if (match.trackId() == null) {
            return RecognitionResult.noMatch(sampleDurationMs);
        }
        return new RecognitionResult(
                true,
                match.trackId(),
                match.confidence(),
                match.matchedAtMs(),
                sampleDurationMs,
                match.hashMatches(),
                match.offsetClusterSize());
    }

    private DecodedAudio decode(InputAudio audio) {
        try (InputStream content = storage.get(audio.bucket(), audio.objectKey())) {
            return decoder.decode(content);
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Could not close audio input", exception);
        }
    }
}
