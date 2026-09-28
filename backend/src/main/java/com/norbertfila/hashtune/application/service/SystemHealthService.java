package com.norbertfila.hashtune.application.service;

import com.norbertfila.hashtune.application.port.in.GetSystemHealthUseCase;
import com.norbertfila.hashtune.domain.health.SystemHealth;
import org.springframework.stereotype.Service;

@Service
public class SystemHealthService implements GetSystemHealthUseCase {

    @Override
    public SystemHealth getHealth() {
        return new SystemHealth("HashTune", "UP");
    }
}
