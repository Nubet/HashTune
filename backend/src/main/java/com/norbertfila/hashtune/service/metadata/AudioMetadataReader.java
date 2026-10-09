package com.norbertfila.hashtune.service.metadata;

import com.drew.imaging.FileTypeDetector;
import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.mp3.Mp3MetadataReader;
import com.drew.metadata.Metadata;
import com.norbertfila.hashtune.configuration.AudioSafetyProperties;
import com.norbertfila.hashtune.exceptions.audio.AudioInputRejectedException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.stream.StreamSupport;
import lombok.RequiredArgsConstructor;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.datatype.Artwork;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
@RequiredArgsConstructor
public class AudioMetadataReader {
    private final AudioSafetyProperties safetyProperties;

    public AudioMetadata read(MultipartFile file) {
        if (isFlac(file)) {
            return readFlac(file);
        }
        if (isM4a(file)) {
            return readM4a(file);
        }
        try (InputStream input = file.getInputStream()) {
            Metadata metadata =
                    isMp3(file) ? Mp3MetadataReader.readMetadata(input) : ImageMetadataReader.readMetadata(input);
            return AudioMetadata.basic(value(metadata, "title"), value(metadata, "artist"), value(metadata, "album"));
        } catch (AudioInputRejectedException exception) {
            throw exception;
        } catch (Exception ignored) {
            return AudioMetadata.empty();
        }
    }

    private AudioMetadata readFlac(MultipartFile file) {
        return readTaggedAudio(file, ".flac");
    }

    private AudioMetadata readM4a(MultipartFile file) {
        return readTaggedAudio(file, ".m4a");
    }

    private AudioMetadata readTaggedAudio(MultipartFile file, String fallbackExtension) {
        Path temporaryFile = null;
        try {
            String name = file.getOriginalFilename();
            String extension = name != null && name.lastIndexOf('.') >= 0
                    ? name.substring(name.lastIndexOf('.'))
                    : fallbackExtension;
            temporaryFile = Files.createTempFile("hashtune-", extension);
            file.transferTo(temporaryFile);
            AudioFile audioFile = AudioFileIO.read(temporaryFile.toFile());
            Tag tag = audioFile.getTag();
            if (tag == null) {
                return AudioMetadata.empty();
            }
            Artwork artwork = tag.getFirstArtwork();
            EmbeddedArtwork embeddedArtwork = artwork == null ? null : readArtwork(artwork);
            return new AudioMetadata(
                    first(tag, FieldKey.TITLE),
                    first(tag, FieldKey.ARTIST),
                    first(tag, FieldKey.ALBUM),
                    first(tag, FieldKey.ALBUM_ARTIST),
                    first(tag, FieldKey.COMPOSER),
                    first(tag, FieldKey.GENRE),
                    first(tag, FieldKey.YEAR),
                    number(first(tag, FieldKey.TRACK)),
                    number(first(tag, FieldKey.DISC_NO)),
                    first(tag, FieldKey.ISRC),
                    first(tag, "BARCODE"),
                    first(tag, FieldKey.COMMENT),
                    embeddedArtwork);
        } catch (AudioInputRejectedException exception) {
            throw exception;
        } catch (Exception ignored) {
            return AudioMetadata.empty();
        } finally {
            if (temporaryFile != null) {
                try {
                    Files.deleteIfExists(temporaryFile);
                } catch (IOException ignored) {
                    // Temporary file cleanup is best effort.
                }
            }
        }
    }

    private EmbeddedArtwork readArtwork(Artwork artwork) {
        String mimeType = artwork.getMimeType() == null
                ? ""
                : artwork.getMimeType().split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
        if (!safetyProperties.getAllowedCoverArtContentTypes().contains(mimeType)) {
            throw new AudioInputRejectedException(
                    "AUDIO_COVER_ART_FORMAT_NOT_SUPPORTED", "Embedded cover art format is not supported");
        }
        byte[] data = artwork.getBinaryData();
        if (data.length > safetyProperties.getMaxCoverArtBytes()) {
            throw new AudioInputRejectedException(
                    "AUDIO_COVER_ART_TOO_LARGE", "Embedded cover art exceeds the maximum allowed size");
        }
        String detectedMime;
        try (InputStream input = new ByteArrayInputStream(data)) {
            detectedMime = FileTypeDetector.detectFileType(input).getMimeType();
        } catch (IOException exception) {
            throw new AudioInputRejectedException(
                    "AUDIO_COVER_ART_FORMAT_NOT_SUPPORTED", "Embedded cover art could not be inspected");
        }
        if (!mimeType.equalsIgnoreCase(detectedMime)) {
            throw new AudioInputRejectedException(
                    "AUDIO_COVER_ART_FORMAT_NOT_SUPPORTED", "Embedded cover art signature is invalid");
        }
        return new EmbeddedArtwork(data, mimeType);
    }

    private boolean isMp3(MultipartFile file) {
        String name = file.getOriginalFilename();
        return name != null && name.toLowerCase(Locale.ROOT).endsWith(".mp3");
    }

    private boolean isFlac(MultipartFile file) {
        String name = file.getOriginalFilename();
        return (name != null && name.toLowerCase(Locale.ROOT).endsWith(".flac"))
                || "audio/flac".equalsIgnoreCase(file.getContentType());
    }

    private boolean isM4a(MultipartFile file) {
        String name = file.getOriginalFilename();
        if (name == null) {
            return false;
        }
        String lowerName = name.toLowerCase(Locale.ROOT);
        return lowerName.endsWith(".m4a") || lowerName.endsWith(".mp4");
    }

    private String first(Tag tag, FieldKey field) {
        return clean(tag.getFirst(field));
    }

    private String first(Tag tag, String field) {
        return clean(tag.getFirst(field));
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private Integer number(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Integer.valueOf(value.split("/", 2)[0].trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String value(Metadata metadata, String field) {
        return StreamSupport.stream(metadata.getDirectories().spliterator(), false)
                .flatMap(directory -> directory.getTags().stream())
                .filter(tag -> tag.getTagName().toLowerCase(Locale.ROOT).equals(field))
                .map(tag -> tag.getDescription())
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse(null);
    }

    public record AudioMetadata(
            String title,
            String artist,
            String album,
            String albumArtist,
            String composer,
            String genre,
            String releaseYear,
            Integer trackNumber,
            Integer discNumber,
            String isrc,
            String barcode,
            String comment,
            EmbeddedArtwork artwork) {
        static AudioMetadata basic(String title, String artist, String album) {
            return new AudioMetadata(title, artist, album, null, null, null, null, null, null, null, null, null, null);
        }

        static AudioMetadata empty() {
            return basic(null, null, null);
        }
    }

    public record EmbeddedArtwork(byte[] data, String mimeType) {}
}
