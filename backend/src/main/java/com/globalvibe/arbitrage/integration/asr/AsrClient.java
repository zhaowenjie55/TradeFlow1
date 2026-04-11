package com.globalvibe.arbitrage.integration.asr;

import com.globalvibe.arbitrage.config.AsrProperties;
import com.globalvibe.arbitrage.domain.asr.dto.AsrSegment;
import com.globalvibe.arbitrage.domain.asr.dto.AsrTranscriptionResult;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Component
public class AsrClient {

    private final AsrProperties asrProperties;
    private final RestClient restClient;

    public AsrClient(RestClient.Builder restClientBuilder, AsrProperties asrProperties) {
        this.asrProperties = asrProperties;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(asrProperties.getConnectTimeoutMillis());
        requestFactory.setReadTimeout(asrProperties.getReadTimeoutMillis());
        this.restClient = restClientBuilder
                .requestFactory(requestFactory)
                .build();
    }

    public AsrTranscriptionResult transcribe(MultipartFile file, String task, String language) {
        if (!asrProperties.isEnabled()) {
            throw new IllegalStateException("ASR service is disabled.");
        }
        if (asrProperties.getEndpoint() == null || asrProperties.getEndpoint().isBlank()) {
            throw new IllegalStateException("ASR endpoint is not configured.");
        }

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", filePart(file));
        body.add("task", task == null || task.isBlank() ? "transcribe" : task);
        if (language != null && !language.isBlank()) {
            body.add("language", language);
        }

        AsrResponsePayload response = restClient.post()
                .uri(asrProperties.getEndpoint())
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .body(AsrResponsePayload.class);

        if (response == null) {
            throw new IllegalStateException("ASR service returned an empty response.");
        }

        return new AsrTranscriptionResult(
                response.success(),
                response.language(),
                response.duration(),
                response.text(),
                response.segments() == null ? List.of() : response.segments().stream()
                        .map(segment -> new AsrSegment(segment.start(), segment.end(), segment.text()))
                        .toList()
        );
    }

    private ByteArrayResource filePart(MultipartFile file) {
        try {
            return new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            };
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to read uploaded media file.", ex);
        }
    }

    private record AsrResponsePayload(
            boolean success,
            String language,
            double duration,
            String text,
            List<AsrSegmentPayload> segments
    ) {
    }

    private record AsrSegmentPayload(
            double start,
            double end,
            String text
    ) {
    }
}

