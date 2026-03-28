package com.globalvibe.arbitrage.config;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.bgesmallzhv15.BgeSmallZhV15EmbeddingModelFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "app.vector", name = "enabled", havingValue = "true", matchIfMissing = true)
public class LangChain4jVectorConfig {

    @Bean
    public EmbeddingModel productEmbeddingModel() {
        return BgeSmallZhV15EmbeddingModelFactory.create();
    }
}
