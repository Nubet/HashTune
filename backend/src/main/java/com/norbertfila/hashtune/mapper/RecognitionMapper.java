package com.norbertfila.hashtune.mapper;

import com.norbertfila.hashtune.dto.response.HistoryResponse;
import com.norbertfila.hashtune.dto.response.RecognitionResponse;
import com.norbertfila.hashtune.entity.recognition.Recognition;
import com.norbertfila.hashtune.entity.track.Track;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

public final class RecognitionMapper {
    private RecognitionMapper() {}

    public static RecognitionResponse toResponse(Recognition recognition, Track track) {
        return new RecognitionResponse(
                recognition.status().name(),
                track == null ? null : TrackMapper.toResponse(track),
                recognition.confidence(),
                recognition.matchedAtMs(),
                recognition.sampleDurationMs(),
                recognition.recognitionTimeMs());
    }

    public static HistoryResponse toHistoryResponse(Recognition recognition, Track track) {
        String recordingUrl = recordingUrl(recognition.id(), false);
        String downloadUrl = recordingUrl(recognition.id(), true);
        return new HistoryResponse(
                recognition.id(),
                track == null ? null : TrackMapper.toResponse(track),
                recognition.status().name(),
                recognition.confidence(),
                recognition.source().name(),
                recognition.recordingObjectKey() == null ? null : recordingUrl,
                recognition.recordingObjectKey() == null ? null : downloadUrl,
                recognition.sampleDurationMs(),
                recognition.createdAt());
    }

    private static String recordingUrl(java.util.UUID id, boolean download) {
        String url = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/recognition-history/{id}/recording")
                .buildAndExpand(id)
                .toUriString();
        return download ? url + "?download=true" : url;
    }
}
