package com.globalvibe.arbitrage.domain.asr.controller;

import com.globalvibe.arbitrage.common.api.ApiResponse;
import com.globalvibe.arbitrage.domain.asr.dto.MediaAnalysisResponse;
import com.globalvibe.arbitrage.domain.asr.dto.VoiceQueryPreviewResponse;
import com.globalvibe.arbitrage.domain.asr.dto.VoiceQueryResponse;
import com.globalvibe.arbitrage.domain.asr.dto.VoiceQueryTextPreviewRequest;
import com.globalvibe.arbitrage.domain.asr.service.AsrApplicationService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class AsrController {

    private final AsrApplicationService asrApplicationService;

    public AsrController(AsrApplicationService asrApplicationService) {
        this.asrApplicationService = asrApplicationService;
    }

    @PostMapping(path = "/voice-query", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<VoiceQueryResponse> voiceQuery(
            @RequestPart("file") MultipartFile file,
            @RequestParam(defaultValue = "transcribe") String task,
            @RequestParam(required = false) String language
    ) {
        return ApiResponse.success(asrApplicationService.handleVoiceQuery(file, task, language));
    }

    @PostMapping(path = "/voice-query/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<VoiceQueryPreviewResponse> previewVoiceQuery(
            @RequestPart("file") MultipartFile file,
            @RequestParam(defaultValue = "transcribe") String task,
            @RequestParam(required = false) String language
    ) {
        return ApiResponse.success(asrApplicationService.previewVoiceQuery(file, task, language));
    }

    @PostMapping(path = "/voice-query/preview/text", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<VoiceQueryPreviewResponse> previewVoiceQueryText(
            @RequestBody VoiceQueryTextPreviewRequest request
    ) {
        return ApiResponse.success(asrApplicationService.previewVoiceQueryText(request.transcript(), request.translatedText()));
    }

    @PostMapping(path = "/media/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<MediaAnalysisResponse> analyzeMedia(
            @RequestPart("file") MultipartFile file,
            @RequestParam(defaultValue = "transcribe") String task,
            @RequestParam(required = false) String language
    ) {
        return ApiResponse.success(asrApplicationService.analyzeMedia(file, task, language));
    }
}
