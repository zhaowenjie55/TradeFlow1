package com.globalvibe.arbitrage;

import com.globalvibe.arbitrage.config.IntegrationGatewayProperties;
import com.globalvibe.arbitrage.config.TaskExecutionProperties;
import com.globalvibe.arbitrage.config.VectorSearchProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({
        TaskExecutionProperties.class,
        IntegrationGatewayProperties.class,
        VectorSearchProperties.class
})
public class ArbitrageApplication {

    public static void main(String[] args) {
        SpringApplication.run(ArbitrageApplication.class, args);
    }
}
