package com.norbertfila.hashtune.controller.indexing;

import com.norbertfila.hashtune.dto.response.IndexingJobResponse;
import com.norbertfila.hashtune.mapper.IndexingJobMapper;
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
    public IndexingJobResponse get(@PathVariable UUID id) {
        return IndexingJobMapper.toResponse(service.get(id));
    }

    @PostMapping("/{id}/retry")
    public IndexingJobResponse retry(@PathVariable UUID id) {
        return IndexingJobMapper.toResponse(service.retry(id));
    }
}
