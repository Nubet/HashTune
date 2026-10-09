package com.norbertfila.hashtune.service.health;

import com.norbertfila.hashtune.entity.health.SystemHealth;
import org.springframework.stereotype.Service;

@Service
public class SystemHealthService implements GetSystemHealthUseCase {

    @Override
    public SystemHealth getHealth() {
        return new SystemHealth("HashTune", "UP");
    }
}
