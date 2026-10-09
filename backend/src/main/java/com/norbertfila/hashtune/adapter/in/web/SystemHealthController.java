package com.norbertfila.hashtune.adapter.in.web;

import com.norbertfila.hashtune.application.port.in.GetSystemHealthUseCase;
import com.norbertfila.hashtune.entity.health.SystemHealth;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system")
public class SystemHealthController {

    private final GetSystemHealthUseCase getSystemHealthUseCase;

    public SystemHealthController(GetSystemHealthUseCase getSystemHealthUseCase) {
        this.getSystemHealthUseCase = getSystemHealthUseCase;
    }

    @GetMapping("/health")
    public SystemHealth getHealth() {
        return getSystemHealthUseCase.getHealth();
    }
}
