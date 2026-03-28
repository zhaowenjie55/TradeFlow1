package com.globalvibe.arbitrage.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.vector")
public class VectorSearchProperties {

    private boolean enabled = true;
    private boolean bootstrapOnStartup = true;
    private String provider = "local-bge-m3";
    private String modelPath;
    private String tokenizerPath;
    private String poolingMode = "CLS";
    private String domesticPlatform = "TAOBAO";
    private String fixedKeyword = "亚克力透明收纳架";
    private int maxResults = 12;
    private double minScore = 0.55D;
    private int dimension = 1024;
}
