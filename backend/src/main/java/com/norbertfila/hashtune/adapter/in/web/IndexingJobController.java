package com.norbertfila.hashtune.adapter.in.web;

import com.norbertfila.hashtune.application.service.TrackApplicationService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/indexing-jobs")
@RequiredArgsConstructor
public class IndexingJobController {
    private final TrackApplicationService service;

    @GetMapping("/{id}")
    public ApiDtos.IndexingJobResponse get(@PathVariable UUID id) {
        return ApiDtos.IndexingJobResponse.from(service.getJob(id));
    }
}
