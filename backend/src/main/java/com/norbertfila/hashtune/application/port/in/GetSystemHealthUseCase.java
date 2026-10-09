package com.norbertfila.hashtune.application.port.in;

import com.norbertfila.hashtune.entity.health.SystemHealth;

public interface GetSystemHealthUseCase {

    SystemHealth getHealth();
}
