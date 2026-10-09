package com.norbertfila.hashtune.service.health;

import static org.assertj.core.api.Assertions.assertThat;

import com.norbertfila.hashtune.entity.health.SystemHealth;
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
