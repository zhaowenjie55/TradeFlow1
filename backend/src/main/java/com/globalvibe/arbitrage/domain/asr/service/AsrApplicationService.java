package com.globalvibe.arbitrage.domain.asr.service;

import com.globalvibe.arbitrage.config.AsrProperties;
import com.globalvibe.arbitrage.domain.asr.dto.AsrTranscriptionResult;
import com.globalvibe.arbitrage.domain.asr.dto.MediaAnalysisResponse;
import com.globalvibe.arbitrage.domain.asr.dto.TranscriptIntentResult;
import com.globalvibe.arbitrage.domain.asr.dto.VoiceQueryPreviewResponse;
import com.globalvibe.arbitrage.domain.asr.dto.VoiceQueryResponse;
import com.globalvibe.arbitrage.domain.search.dto.SearchRequest;
import com.globalvibe.arbitrage.domain.search.dto.SearchResponse;
import com.globalvibe.arbitrage.domain.search.service.SearchService;
import com.globalvibe.arbitrage.integration.asr.AsrClient;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class AsrApplicationService {

    private static final Set<String> GENERIC_KEYWORDS = Set.of(
            "general sourcing",
            "product sourcing",
            "media analysis",
            "voice query",
            "unknown"
    );

    private final AsrProperties asrProperties;
    private final AsrClient asrClient;
    private final TranscriptIntentService transcriptIntentService;
    private final SearchService searchService;
    private final VoiceTermLexicon voiceTermLexicon;

    public AsrApplicationService(
            AsrProperties asrProperties,
            AsrClient asrClient,
            TranscriptIntentService transcriptIntentService,
            SearchService searchService,
            VoiceTermLexicon voiceTermLexicon
    ) {
        this.asrProperties = asrProperties;
        this.asrClient = asrClient;
        this.transcriptIntentService = transcriptIntentService;
        this.searchService = searchService;
        this.voiceTermLexicon = voiceTermLexicon;
    }

    public VoiceQueryResponse handleVoiceQuery(MultipartFile file, String task, String language) {
        VoiceQueryPreviewResponse preview = previewVoiceQuery(file, task, language);
        SearchResponse searchResults = searchService.searchAmazon(new SearchRequest(preview.normalizedKeyword(), 1));
        return new VoiceQueryResponse(preview.transcript(), preview.translatedText(), preview.intent(), preview.normalizedKeyword(), searchResults);
    }

    public VoiceQueryPreviewResponse previewVoiceQuery(MultipartFile file, String task, String language) {
        validateFile(file);
        String resolvedLanguage = normalizeVoiceLanguage(language);
        AsrTranscriptionResult transcript = asrClient.transcribe(file, task, resolvedLanguage);
        String translatedText = resolveTranslatedText(file, task, resolvedLanguage, transcript);
        return buildPreviewResponse(transcript, translatedText);
    }

    public VoiceQueryPreviewResponse previewVoiceQueryText(String transcriptText, String translatedText) {
        String normalizedTranscript = normalizeTranscriptForVoice(transcriptText);
        if (normalizedTranscript.isBlank()) {
            throw new IllegalArgumentException("识别文本不能为空，请先输入或重新录音。");
        }
        AsrTranscriptionResult transcript = new AsrTranscriptionResult(
                true,
                null,
                0,
                normalizedTranscript,
                List.of()
        );
        String resolvedTranslatedText = normalizeEnglishSearchText(translatedText);
        return buildPreviewResponse(transcript, resolvedTranslatedText);
    }

    public MediaAnalysisResponse analyzeMedia(MultipartFile file, String task, String language) {
        validateFile(file);
        AsrTranscriptionResult transcript = asrClient.transcribe(file, task, language);
        TranscriptIntentResult intent = transcriptIntentService.analyze(transcript.text(), "media-analyze");
        return new MediaAnalysisResponse(transcript, intent);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded media file must not be empty.");
        }
        long maxBytes = (long) asrProperties.getMaxFileSizeMb() * 1024 * 1024;
        if (file.getSize() > maxBytes) {
            throw new IllegalArgumentException("Uploaded media file exceeds the configured size limit.");
        }
    }

    private String resolveTranslatedText(MultipartFile file, String task, String language, AsrTranscriptionResult transcript) {
        if (task != null && task.equalsIgnoreCase("translate")) {
            return normalizeText(transcript.text());
        }
        if (!requiresTranslation(transcript)) {
            return normalizeText(transcript.text());
        }

        AsrTranscriptionResult translated = asrClient.transcribe(file, "translate", language);
        String translatedText = normalizeText(translated.text());
        return translatedText.isBlank() ? normalizeText(transcript.text()) : translatedText;
    }

    private VoiceQueryPreviewResponse buildPreviewResponse(AsrTranscriptionResult transcript, String translatedText) {
        String transcriptText = normalizeTranscriptForVoice(transcript.text());
        TranscriptIntentResult transcriptIntent = transcriptIntentService.analyze(transcriptText, "voice-query");
        String resolvedTranslatedText = normalizeEnglishSearchText(translatedText);
        if (shouldReplaceTranslatedPreviewText(transcriptText, resolvedTranslatedText)) {
            resolvedTranslatedText = resolveTranslatedPreviewText(transcriptText, transcriptIntent);
        }
        resolvedTranslatedText = normalizeEnglishSearchText(resolvedTranslatedText);
        TranscriptIntentResult intent = resolvedTranslatedText.equals(transcriptText)
                ? transcriptIntent
                : transcriptIntentService.analyze(resolvedTranslatedText, "voice-query");
        String normalizedKeyword = resolveNormalizedKeyword(intent, resolvedTranslatedText);
        return new VoiceQueryPreviewResponse(transcript, resolvedTranslatedText, intent, normalizedKeyword);
    }

    private String resolveTranslatedPreviewText(String transcriptText, TranscriptIntentResult intent) {
        if (transcriptText.isBlank()) {
            return "";
        }
        String mappedFromChinese = mapChineseProductTextToEnglish(transcriptText);
        if (!mappedFromChinese.isBlank()) {
            return mappedFromChinese;
        }
        if (!requiresTranslationText(transcriptText)) {
            return transcriptText;
        }
        List<String> keywords = intent.keywords() == null ? List.of() : intent.keywords();
        for (String keyword : keywords) {
            if (isUsableKeyword(keyword)) {
                return keyword.trim();
            }
        }
        if (isUsableKeyword(intent.category())) {
            return intent.category().trim();
        }
        return transcriptText;
    }

    private boolean shouldReplaceTranslatedPreviewText(String transcriptText, String translatedText) {
        if (translatedText.isBlank()) {
            return true;
        }
        boolean transcriptContainsHan = requiresTranslationText(transcriptText);
        boolean translatedContainsHan = requiresTranslationText(translatedText);
        if (transcriptContainsHan && translatedContainsHan) {
            return true;
        }
        if (transcriptContainsHan && translatedText.equalsIgnoreCase(transcriptText)) {
            return true;
        }
        return translatedText.length() <= 2;
    }

    private boolean requiresTranslation(AsrTranscriptionResult transcript) {
        if (transcript == null) {
            return false;
        }
        String language = transcript.language();
        if (language != null && language.toLowerCase(Locale.ROOT).startsWith("zh")) {
            return true;
        }
        String text = transcript.text();
        return text != null && text.codePoints().anyMatch(codePoint -> Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HAN);
    }

    private boolean requiresTranslationText(String text) {
        return text != null && text.codePoints().anyMatch(codePoint -> Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HAN);
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.replaceAll("\\s+", " ").trim();
    }

    private String normalizeTranscriptForVoice(String value) {
        String normalized = normalizeText(value);
        if (normalized.isBlank()) {
            return normalized;
        }
        for (Map.Entry<String, String> entry : voiceTermLexicon.zhCorrections().entrySet()) {
            normalized = normalized.replace(entry.getKey(), entry.getValue());
        }
        return normalized;
    }

    private String normalizeEnglishSearchText(String value) {
        String normalized = normalizeText(value)
                .replaceAll("\\s+", " ")
                .trim();
        if (normalized.isBlank()) {
            return normalized;
        }
        String lower = normalized.toLowerCase(Locale.ROOT);
        for (Map.Entry<String, String> entry : voiceTermLexicon.enCorrections().entrySet()) {
            if (lower.contains(entry.getKey())) {
                lower = lower.replace(entry.getKey(), entry.getValue());
            }
        }
        return lower.replaceAll("\\s+", " ").trim();
    }

    private String normalizeVoiceLanguage(String language) {
        String normalized = normalizeText(language);
        return normalized.isBlank() ? "zh" : normalized;
    }

    private String resolveNormalizedKeyword(TranscriptIntentResult intent, String transcript) {
        List<String> keywords = intent.keywords() == null ? List.of() : intent.keywords();
        for (String keyword : keywords) {
            if (isUsableKeyword(keyword)) {
                return normalizeEnglishSearchText(keyword);
            }
        }
        if (isUsableKeyword(intent.category())) {
            return normalizeEnglishSearchText(intent.category());
        }
        return normalizeEnglishSearchText(abbreviateTranscript(transcript));
    }

    private boolean isUsableKeyword(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return !GENERIC_KEYWORDS.contains(normalized);
    }

    private String abbreviateTranscript(String transcript) {
        String normalized = transcript == null ? "" : transcript.replaceAll("\\s+", " ").trim();
        if (normalized.isBlank()) {
            return "voice query";
        }
        if (normalized.length() <= 2) {
            throw new IllegalArgumentException("语音内容过短，无法生成有效搜索词，请重试并说完整的商品需求。");
        }
        return normalized.length() <= 80 ? normalized : normalized.substring(0, 80).trim();
    }

    private String mapChineseProductTextToEnglish(String text) {
        String normalized = normalizeTranscriptForVoice(text);
        for (Map.Entry<String, String> entry : voiceTermLexicon.zhToEnTerms().entrySet()) {
            if (normalized.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return "";
    }
}
