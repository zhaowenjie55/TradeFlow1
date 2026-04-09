package com.globalvibe.arbitrage.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.integration")
public class IntegrationGatewayProperties {

    private LlmProperties llm = new LlmProperties();
    private CrawlerProperties crawler = new CrawlerProperties();
    private DomesticProperties domestic = new DomesticProperties();
    private OverseasProperties overseas = new OverseasProperties();

    @Data
    public static class LlmProperties {
        private boolean enabled = true;
        private boolean forceSimulated = false;
        private String endpointBase;
        private int connectTimeoutMillis = 5000;
        private int readTimeoutMillis = 20000;
        private String chatEndpoint;
        private String model = "glm-5";
        private String apiKey;
        private double temperature = 0.2D;
    }

    @Data
    public static class CrawlerProperties {
        private boolean enabled = true;
        private String searchEndpoint;
        private String detailEndpoint;
    }

    @Data
    public static class DomesticProperties {
        private boolean enabled = false;
        private boolean forceFallback = true;
        private String searchEndpoint;
        private String detailEndpoint;
        private String apiKey;
        private int connectTimeoutMillis = 5000;
        private int readTimeoutMillis = 300000;
    }

    @Data
    public static class OverseasProperties {
        private boolean enabled = false;
        private boolean forceFallback = true;
        private String searchEndpoint;
        private String apiKey;
    }
}
