package com.norbertfila.hashtune.adapter.out.persistence;

import com.norbertfila.hashtune.entity.recognition.RecognitionEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataRecognitionRepository extends JpaRepository<RecognitionEntity, UUID> {
    Page<RecognitionEntity> findByOwnerIssuerAndOwnerSubject(String issuer, String subject, Pageable pageable);

    List<RecognitionEntity> findAllByOwnerIssuerAndOwnerSubjectOrderByCreatedAtDesc(String issuer, String subject);

    Optional<RecognitionEntity> findByIdAndOwnerIssuerAndOwnerSubject(UUID id, String issuer, String subject);

    void deleteAllByOwnerIssuerAndOwnerSubject(String issuer, String subject);
}
