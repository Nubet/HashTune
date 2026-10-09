package com.norbertfila.hashtune.adapter.in.web;

import com.norbertfila.hashtune.service.indexing.IndexingJobService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/indexing-jobs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class IndexingJobController {
    private final IndexingJobService service;

    @GetMapping("/{id}")
    public ApiDtos.IndexingJobResponse get(@PathVariable UUID id) {
        return ApiDtos.IndexingJobResponse.from(service.get(id));
    }

    @PostMapping("/{id}/retry")
    public ApiDtos.IndexingJobResponse retry(@PathVariable UUID id) {
        return ApiDtos.IndexingJobResponse.from(service.retry(id));
    }
}
