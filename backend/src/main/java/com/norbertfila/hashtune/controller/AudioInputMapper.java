package com.norbertfila.hashtune.controller;

import com.norbertfila.hashtune.service.audio.AudioInput;
import org.springframework.web.multipart.MultipartFile;

public final class AudioInputMapper {
    private AudioInputMapper() {}

    public static AudioInput from(MultipartFile file) {
        return new AudioInput(file.getOriginalFilename(), file.getContentType(), file.getSize(), file::getInputStream);
    }
}
