package com.norbertfila.hashtune.adapter.out.fingerprinting;

import com.norbertfila.hashtune.application.port.out.AudioRecognitionEngine;
import com.norbertfila.hashtune.application.port.out.FingerprintRepository;
import com.norbertfila.hashtune.application.port.out.FingerprintRepository.FingerprintMatch;
import com.norbertfila.hashtune.application.port.out.ObjectStoragePort;
import com.norbertfila.hashtune.configuration.AudioProperties;
import com.norbertfila.hashtune.configuration.AudioSafetyProperties;
import com.norbertfila.hashtune.domain.fingerprint.FingerprintOccurrence;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.audio.engine", havingValue = "spectral")
@Slf4j
public class SpectralAudioRecognitionEngine implements AudioRecognitionEngine {
    private static final long MILLISECONDS_PER_SECOND = 1_000;

    private final ObjectStoragePort storage;
    private final FfmpegAudioDecoder decoder;
    private final SpectralFingerprinting fingerprinting;
    private final FingerprintRepository fingerprints;
    private final FingerprintMatcher matcher;

    public SpectralAudioRecognitionEngine(
            ObjectStoragePort storage,
            AudioProperties audioProperties,
            AudioSafetyProperties safetyProperties,
            FingerprintRepository fingerprints) {
        this.storage = storage;
        this.decoder = new FfmpegAudioDecoder(
                audioProperties.getFfmpegBinary(),
                safetyProperties.getFfprobeBinary(),
                safetyProperties);
        this.fingerprinting = new SpectralFingerprinting(
                audioProperties.getFingerprint().getMaxPeaksPerSecond(),
                audioProperties.getFingerprint().getFanOut());
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
        long startedNanos = System.nanoTime();
        long decodeStartedNanos = System.nanoTime();
        DecodedAudio decodedAudio = decode(audio);
        long decodeMs = elapsedMs(decodeStartedNanos);
        long fingerprintStartedNanos = System.nanoTime();
        List<FingerprintOccurrence> sampleFingerprints =
                fingerprinting.fingerprint(new AudioSamples(decodedAudio.samples(), decodedAudio.sampleRate()));
        long fingerprintMs = elapsedMs(fingerprintStartedNanos);
        List<Long> distinctHashes = sampleFingerprints.stream()
                .map(FingerprintOccurrence::hash)
                .distinct()
                .toList();
        long lookupStartedNanos = System.nanoTime();
        List<FingerprintMatch> storedMatches = fingerprints.findMatches(distinctHashes);
        long lookupMs = elapsedMs(lookupStartedNanos);
        long matchStartedNanos = System.nanoTime();
        FingerprintMatcher.MatchResult match = matcher.match(sampleFingerprints, storedMatches);
        long matchMs = elapsedMs(matchStartedNanos);
        long sampleDurationMs = Math.round(decodedAudio.durationSeconds() * MILLISECONDS_PER_SECOND);
        log.info(
                "recognition_benchmark sampleDurationMs={} fingerprintCount={} distinctHashCount={} dbRowsFetched={} decodeMs={} fingerprintMs={} lookupMs={} matchMs={} engineTotalMs={} matched={}",
                sampleDurationMs,
                sampleFingerprints.size(),
                distinctHashes.size(),
                storedMatches.size(),
                decodeMs,
                fingerprintMs,
                lookupMs,
                matchMs,
                elapsedMs(startedNanos),
                match.trackId() != null);
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

    private long elapsedMs(long startedNanos) {
        return (System.nanoTime() - startedNanos) / 1_000_000;
    }

    private DecodedAudio decode(InputAudio audio) {
        try (InputStream content = storage.get(audio.bucket(), audio.objectKey())) {
            return decoder.decode(content);
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Could not close audio input", exception);
        }
    }
}
