package com.norbertfila.hashtune.service.health;

import com.norbertfila.hashtune.entity.health.SystemHealth;

public interface GetSystemHealthUseCase {

    SystemHealth getHealth();
}
