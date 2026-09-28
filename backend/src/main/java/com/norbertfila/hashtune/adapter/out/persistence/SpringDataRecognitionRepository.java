package com.norbertfila.hashtune.adapter.out.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataRecognitionRepository extends JpaRepository<RecognitionEntity, UUID> {}
