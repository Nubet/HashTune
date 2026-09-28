package com.norbertfila.hashtune.application.port.in;

import com.norbertfila.hashtune.domain.health.SystemHealth;

public interface GetSystemHealthUseCase {

    SystemHealth getHealth();
}
