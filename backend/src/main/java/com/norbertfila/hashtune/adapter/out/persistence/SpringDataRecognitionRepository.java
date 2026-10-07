package com.norbertfila.hashtune.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataRecognitionRepository extends JpaRepository<RecognitionEntity, UUID> {
    Page<RecognitionEntity> findBySessionId(String sessionId, Pageable pageable);

    List<RecognitionEntity> findAllBySessionIdOrderByCreatedAtDesc(String sessionId);

    Optional<RecognitionEntity> findByIdAndSessionId(UUID id, String sessionId);

    void deleteAllBySessionId(String sessionId);
}
