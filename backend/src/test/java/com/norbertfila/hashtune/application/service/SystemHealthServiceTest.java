package com.norbertfila.hashtune.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.norbertfila.hashtune.domain.health.SystemHealth;
import org.junit.jupiter.api.Test;

class SystemHealthServiceTest {

    private final SystemHealthService service = new SystemHealthService();

    @Test
    void returnsHealthyHashTuneStatus() {
        SystemHealth health = service.getHealth();

        assertThat(health.application()).isEqualTo("HashTune");
        assertThat(health.status()).isEqualTo("UP");
    }
}
