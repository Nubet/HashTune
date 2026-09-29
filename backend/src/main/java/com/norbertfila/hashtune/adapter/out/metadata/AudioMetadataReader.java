package com.norbertfila.hashtune.adapter.out.metadata;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.mp3.Mp3MetadataReader;
import com.drew.metadata.Metadata;
import java.io.InputStream;
import java.util.Locale;
import java.util.stream.StreamSupport;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class AudioMetadataReader {
    public AudioMetadata read(MultipartFile file) {
        try (InputStream input = file.getInputStream()) {
            Metadata metadata =
                    isMp3(file) ? Mp3MetadataReader.readMetadata(input) : ImageMetadataReader.readMetadata(input);
            return new AudioMetadata(value(metadata, "title"), value(metadata, "artist"), value(metadata, "album"));
        } catch (Exception ignored) {
            return new AudioMetadata(null, null, null);
        }
    }

    private boolean isMp3(MultipartFile file) {
        String name = file.getOriginalFilename();
        return name != null && name.toLowerCase(Locale.ROOT).endsWith(".mp3");
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

    public record AudioMetadata(String title, String artist, String album) {}
}
