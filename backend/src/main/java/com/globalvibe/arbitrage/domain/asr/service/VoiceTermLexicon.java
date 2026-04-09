package com.globalvibe.arbitrage.domain.asr.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class VoiceTermLexicon {

    private final Map<String, String> zhCorrections;
    private final Map<String, String> zhToEnTerms;
    private final Map<String, String> enCorrections;

    public VoiceTermLexicon(ObjectMapper objectMapper) {
        LexiconResource resource = loadResource(objectMapper);
        this.zhCorrections = Map.copyOf(resource.zhCorrections());
        this.zhToEnTerms = Map.copyOf(resource.zhToEnTerms());
        this.enCorrections = Map.copyOf(resource.enCorrections());
    }

    public Map<String, String> zhCorrections() {
        return zhCorrections;
    }

    public Map<String, String> zhToEnTerms() {
        return zhToEnTerms;
    }

    public Map<String, String> enCorrections() {
        return enCorrections;
    }

    private LexiconResource loadResource(ObjectMapper objectMapper) {
        ClassPathResource resource = new ClassPathResource("asr-term-lexicon.json");
        try (InputStream inputStream = resource.getInputStream()) {
            Map<String, Map<String, String>> raw = objectMapper.readValue(
                    inputStream,
                    new TypeReference<>() {
                    }
            );
            return new LexiconResource(
                    new LinkedHashMap<>(raw.getOrDefault("zhCorrections", Map.of())),
                    new LinkedHashMap<>(raw.getOrDefault("zhToEnTerms", Map.of())),
                    new LinkedHashMap<>(raw.getOrDefault("enCorrections", Map.of()))
            );
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load ASR term lexicon.", ex);
        }
    }

    private record LexiconResource(
            Map<String, String> zhCorrections,
            Map<String, String> zhToEnTerms,
            Map<String, String> enCorrections
    ) {
    }
}
