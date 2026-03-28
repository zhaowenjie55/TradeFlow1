package com.globalvibe.arbitrage.domain.product.service;

import com.globalvibe.arbitrage.config.VectorSearchProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "app.vector", name = {"enabled", "bootstrap-on-startup"}, havingValue = "true")
public class DomesticVectorIndexBootstrapper implements ApplicationRunner {

    private final VectorSearchProperties properties;
    private final DomesticVectorSearchService domesticVectorSearchService;

    public DomesticVectorIndexBootstrapper(
            VectorSearchProperties properties,
            DomesticVectorSearchService domesticVectorSearchService
    ) {
        this.properties = properties;
        this.domesticVectorSearchService = domesticVectorSearchService;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("Bootstrapping semantic index for {} using fixed keyword [{}]",
                properties.getDomesticPlatform(),
                properties.getFixedKeyword());
        domesticVectorSearchService.rebuildDomesticIndex();
    }
}
