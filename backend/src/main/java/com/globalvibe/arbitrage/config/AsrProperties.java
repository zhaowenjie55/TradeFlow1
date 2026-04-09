package com.globalvibe.arbitrage.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.integration.asr")
public class AsrProperties {

    private boolean enabled = true;
    private String endpoint = "http://127.0.0.1:8001/api/asr/transcribe";
    private int connectTimeoutMillis = 5_000;
    private int readTimeoutMillis = 120_000;
    private int maxFileSizeMb = 100;
}
