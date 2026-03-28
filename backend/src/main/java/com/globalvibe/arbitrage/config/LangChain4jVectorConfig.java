package com.globalvibe.arbitrage.config;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.OnnxEmbeddingModel;
import dev.langchain4j.model.embedding.onnx.PoolingMode;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

@Configuration
@ConditionalOnProperty(prefix = "app.vector", name = "enabled", havingValue = "true", matchIfMissing = true)
public class LangChain4jVectorConfig {

    private final VectorSearchProperties properties;

    public LangChain4jVectorConfig(VectorSearchProperties properties) {
        this.properties = properties;
    }

    @Bean
    public EmbeddingModel productEmbeddingModel() {
        if (!"local-bge-m3".equalsIgnoreCase(properties.getProvider())) {
            throw new IllegalStateException("Unsupported vector provider: " + properties.getProvider());
        }
        Path modelPath = requirePath(properties.getModelPath(), "VECTOR_MODEL_PATH", "model.onnx");
        Path tokenizerPath = requirePath(properties.getTokenizerPath(), "VECTOR_TOKENIZER_PATH", "tokenizer.json");
        PoolingMode poolingMode = PoolingMode.valueOf(properties.getPoolingMode().trim().toUpperCase(Locale.ROOT));
        return new OnnxEmbeddingModel(
                modelPath.toAbsolutePath().toString(),
                tokenizerPath.toAbsolutePath().toString(),
                poolingMode
        );
    }

    private Path requirePath(String configuredPath, String envName, String expectedFileName) {
        if (configuredPath == null || configuredPath.isBlank()) {
            throw new IllegalStateException("BGE-M3 model/tokenizer not configured: set " + envName + " to local " + expectedFileName);
        }
        Path path = Path.of(configuredPath).toAbsolutePath().normalize();
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            throw new IllegalStateException("BGE-M3 model/tokenizer not configured: " + envName + " file not found at " + path);
        }
        return path;
    }
}
