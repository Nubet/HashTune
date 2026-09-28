package com.norbertfila.hashtune.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.seed.mtg-jamendo")
public class MtgJamendoSeedProperties {
    private boolean enabled;
    private String directory = "../data/dev/mtg_jamendo_mini";
}
