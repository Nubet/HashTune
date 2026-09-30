package com.norbertfila.hashtune.adapter.in.seed;

import com.norbertfila.hashtune.application.port.out.CoverArtProvider;
import com.norbertfila.hashtune.application.port.out.FingerprintRepository;
import com.norbertfila.hashtune.application.port.out.IndexingJobRepository;
import com.norbertfila.hashtune.application.port.out.ObjectStoragePort;
import com.norbertfila.hashtune.application.port.out.TrackRepository;
import com.norbertfila.hashtune.configuration.MtgJamendoSeedProperties;
import com.norbertfila.hashtune.configuration.StorageProperties;
import com.norbertfila.hashtune.domain.indexing.IndexingJob;
import com.norbertfila.hashtune.domain.indexing.IndexingJobStatus;
import com.norbertfila.hashtune.domain.track.Track;
import com.norbertfila.hashtune.domain.track.TrackOrigin;
import com.norbertfila.hashtune.domain.track.TrackStatus;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.seed.mtg-jamendo.enabled", havingValue = "true")
public class MtgJamendoSeeder implements ApplicationRunner {
    private static final String MANIFEST = "metadata.csv";

    private final MtgJamendoSeedProperties properties;
    private final ObjectStoragePort storage;
    private final StorageProperties storageProperties;
    private final TrackRepository tracks;
    private final IndexingJobRepository jobs;
    private final CoverArtProvider coverArtProvider;
    private final FingerprintRepository fingerprints;

    @Override
    public void run(ApplicationArguments args) throws IOException {
        Path directory = Path.of(properties.getDirectory()).toAbsolutePath().normalize();
        List<Map<String, String>> rows = parseCsv(Files.readString(directory.resolve(MANIFEST)));
        int imported = 0;
        int skipped = 0;

        for (Map<String, String> row : rows) {
            Path audio = resolveAudio(directory, required(row, "audio_path"));
            String checksum = sha256(audio);
            Optional<Track> existing = tracks.findByChecksum(checksum);
            if (existing.isPresent()) {
                Track existingTrack = markAsMtgJamendo(existing.get());
                refreshCoverArt(existingTrack);
                if (!jobs.existsByTrackId(existingTrack.id())
                        || (fingerprints.countByTrackId(existing.get().id()) == 0
                                && !jobs.existsByTrackIdAndStatusIn(
                                        existingTrack.id(),
                                        List.of(IndexingJobStatus.PENDING, IndexingJobStatus.PROCESSING)))) {
                    Instant now = Instant.now();
                    jobs.save(new IndexingJob(
                            UUID.randomUUID(),
                            existingTrack.id(),
                            IndexingJobStatus.PENDING,
                            0,
                            0,
                            null,
                            null,
                            now,
                            null,
                            null));
                }
                skipped++;
                continue;
            }

            UUID id = UUID.nameUUIDFromBytes(
                    ("mtg-jamendo:" + required(row, "track_id")).getBytes(StandardCharsets.UTF_8));
            String objectKey = "audio/" + id + "/original-" + audio.getFileName();
            try (InputStream input = Files.newInputStream(audio)) {
                storage.put(storageProperties.getAudioBucket(), objectKey, input, Files.size(audio), "audio/mpeg");
            }

            Instant now = Instant.now();
            Track track = tracks.save(new Track(
                    id,
                    required(row, "title"),
                    required(row, "artist"),
                    required(row, "album"),
                    TrackOrigin.MTG_JAMENDO,
                    null,
                    Math.round(Double.parseDouble(required(row, "duration_seconds")) * 1000),
                    objectKey,
                    checksum,
                    TrackStatus.UPLOADED,
                    now,
                    now));
            track = refreshCoverArt(track);
            jobs.save(new IndexingJob(
                    UUID.randomUUID(), track.id(), IndexingJobStatus.PENDING, 0, 0, null, null, now, null, null));
            imported++;
        }

        log.info("MTG-Jamendo seed complete: imported={}, skipped={}", imported, skipped);
    }

    private Track refreshCoverArt(Track track) {
        if (track.coverArtUrl() != null) {
            return track;
        }
        return coverArtProvider
                .findCoverArt(track.title(), track.artist(), track.album())
                .map(url -> tracks.save(new Track(
                        track.id(),
                        track.title(),
                        track.artist(),
                        track.album(),
                        track.origin(),
                        track.albumArtist(),
                        track.composer(),
                        track.genre(),
                        track.releaseYear(),
                        track.trackNumber(),
                        track.discNumber(),
                        track.isrc(),
                        track.barcode(),
                        track.comment(),
                        url,
                        track.coverArtObjectKey(),
                        track.coverArtMimeType(),
                        track.durationMs(),
                        track.audioObjectKey(),
                        track.checksum(),
                        track.status(),
                        track.createdAt(),
                        Instant.now())))
                .orElse(track);
    }

    private Track markAsMtgJamendo(Track track) {
        if (track.origin() == TrackOrigin.MTG_JAMENDO) {
            return track;
        }
        return tracks.save(new Track(
                track.id(),
                track.title(),
                track.artist(),
                track.album(),
                TrackOrigin.MTG_JAMENDO,
                track.albumArtist(),
                track.composer(),
                track.genre(),
                track.releaseYear(),
                track.trackNumber(),
                track.discNumber(),
                track.isrc(),
                track.barcode(),
                track.comment(),
                track.coverArtUrl(),
                track.coverArtObjectKey(),
                track.coverArtMimeType(),
                track.durationMs(),
                track.audioObjectKey(),
                track.checksum(),
                track.status(),
                track.createdAt(),
                Instant.now()));
    }

    static List<Map<String, String>> parseCsv(String input) {
        List<List<String>> records = new ArrayList<>();
        List<String> record = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;

        for (int index = 0; index < input.length(); index++) {
            char character = input.charAt(index);
            if (character == '"') {
                if (quoted && index + 1 < input.length() && input.charAt(index + 1) == '"') {
                    field.append('"');
                    index++;
                } else {
                    quoted = !quoted;
                }
            } else if (character == ',' && !quoted) {
                record.add(field.toString());
                field.setLength(0);
            } else if (character == '\n' && !quoted) {
                record.add(field.toString().replaceFirst("\\r$", ""));
                field.setLength(0);
                if (record.stream().anyMatch(value -> !value.isBlank())) {
                    records.add(record);
                }
                record = new ArrayList<>();
            } else {
                field.append(character);
            }
        }

        if (field.length() > 0 || !record.isEmpty()) {
            record.add(field.toString().replaceFirst("\\r$", ""));
            records.add(record);
        }
        if (records.isEmpty()) {
            return List.of();
        }

        List<String> headers = records.getFirst();
        headers.set(0, headers.getFirst().replace("\uFEFF", ""));
        return records.subList(1, records.size()).stream()
                .map(values -> {
                    if (values.size() != headers.size()) {
                        throw new IllegalArgumentException("Invalid MTG-Jamendo metadata row");
                    }
                    Map<String, String> row = new HashMap<>();
                    for (int index = 0; index < headers.size(); index++) {
                        row.put(headers.get(index), values.get(index));
                    }
                    return row;
                })
                .toList();
    }

    private Path resolveAudio(Path directory, String relativePath) {
        Path audio = directory.resolve(relativePath).normalize();
        if (!audio.startsWith(directory) || !Files.isRegularFile(audio)) {
            throw new IllegalArgumentException("MTG-Jamendo audio file not found: " + relativePath);
        }
        return audio;
    }

    private String sha256(Path path) throws IOException {
        try (InputStream input = Files.newInputStream(path)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            input.transferTo(new java.security.DigestOutputStream(java.io.OutputStream.nullOutputStream(), digest));
            return java.util.HexFormat.of().formatHex(digest.digest());
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private String required(Map<String, String> row, String field) {
        String value = row.get(field);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing MTG-Jamendo metadata field: " + field);
        }
        return value;
    }
}
