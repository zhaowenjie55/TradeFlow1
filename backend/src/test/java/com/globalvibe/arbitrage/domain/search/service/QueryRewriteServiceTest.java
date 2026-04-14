package com.globalvibe.arbitrage.domain.search.service;

import com.globalvibe.arbitrage.config.VectorSearchProperties;
import com.globalvibe.arbitrage.domain.search.model.QueryRewrite;
import com.globalvibe.arbitrage.domain.search.repository.QueryRewriteRepository;
import com.globalvibe.arbitrage.domain.task.model.TaskMode;
import com.globalvibe.arbitrage.integration.llm.LLMGateway;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QueryRewriteServiceTest {

    @Test
    void shouldPreferDomesticFallbackForCoffeeWhenRewriteIsEnglishOnly() {
        VectorSearchProperties properties = new VectorSearchProperties();
        QueryRewriteRepository repository = new InMemoryQueryRewriteRepository();
        LLMGateway llmGateway = new StubLlmGateway(new LLMGateway.RewriteResult(
                "kauai hawaiian ground coffee",
                List.of("kauai hawaiian ground coffee", "kauai", "coffee"),
                true,
                "SIMULATED_LLM",
                "simulated-llm",
                "fallback",
                OffsetDateTime.now()
        ));

        QueryRewriteService service = new QueryRewriteService(llmGateway, repository, properties);
        QueryRewrite rewrite = service.rewrite(
                "phase2-1",
                "phase1-1:amz-coffee-01",
                "amz-coffee-01",
                "Kauai Hawaiian Ground Coffee",
                TaskMode.AUTO_FALLBACK
        ).queryRewrite();

        assertEquals("夏威夷咖啡粉", rewrite.rewrittenText());
        assertTrue(rewrite.keywords().contains("夏威夷咖啡粉"));
        assertFalse(rewrite.keywords().contains("kauai"));
    }

    @Test
    void shouldNotInjectFixedKeywordWhenConfigurationIsBlank() {
        VectorSearchProperties properties = new VectorSearchProperties();
        QueryRewriteRepository repository = new InMemoryQueryRewriteRepository();
        LLMGateway llmGateway = new StubLlmGateway(new LLMGateway.RewriteResult(
                "桌面收纳架",
                List.of("桌面收纳架", "透明收纳架"),
                false,
                "GLM_CHAT",
                "glm-5",
                null,
                OffsetDateTime.now()
        ));

        QueryRewriteService service = new QueryRewriteService(llmGateway, repository, properties);
        QueryRewrite rewrite = service.rewrite(
                "phase2-1",
                "phase1-1:amz-acrylic-01",
                "amz-acrylic-01",
                "Acrylic Desktop Organizer"
        ).queryRewrite();

        assertFalse(rewrite.keywords().contains("亚克力透明收纳架"));
        assertEquals(List.of("桌面收纳架", "透明收纳架"), rewrite.keywords());
    }

    @Test
    void shouldRejectSimulatedRewriteInRealMode() {
        VectorSearchProperties properties = new VectorSearchProperties();
        QueryRewriteRepository repository = new InMemoryQueryRewriteRepository();
        LLMGateway llmGateway = new StubLlmGateway(new LLMGateway.RewriteResult(
                "桌面收纳架",
                List.of("桌面收纳架"),
                true,
                "SIMULATED_LLM",
                "simulated-llm",
                "LLM 改写接口未启用",
                OffsetDateTime.now()
        ));

        QueryRewriteService service = new QueryRewriteService(llmGateway, repository, properties);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> service.rewrite(
                "phase2-1",
                "phase1-1:amz-acrylic-01",
                "amz-acrylic-01",
                "Acrylic Desktop Organizer",
                TaskMode.REAL
        ));

        assertTrue(exception.getMessage().contains("真实 GLM 改写失败"));
    }

    @Test
    void shouldPreferAnchoredCoffeeQueryWhenLlmRewriteDropsFlavorAndFormat() {
        VectorSearchProperties properties = new VectorSearchProperties();
        QueryRewriteRepository repository = new InMemoryQueryRewriteRepository();
        LLMGateway llmGateway = new StubLlmGateway(new LLMGateway.RewriteResult(
                "夏威夷咖啡豆",
                List.of("夏威夷咖啡豆", "香草夏威夷果风味咖啡", "咖啡粉"),
                false,
                "GLM_CHAT",
                "deepseek-chat",
                null,
                OffsetDateTime.now()
        ));

        QueryRewriteService service = new QueryRewriteService(llmGateway, repository, properties);
        QueryRewrite rewrite = service.rewrite(
                "phase2-1",
                "phase1-1:amz-coffee-02",
                "amz-coffee-02",
                "Kauai Hawaiian Ground Coffee, Vanilla Macadamia Nut Flavor"
        ).queryRewrite();

        assertEquals("香草夏威夷果风味咖啡粉", rewrite.rewrittenText());
        assertTrue(rewrite.keywords().contains("夏威夷果风味咖啡粉"));
        assertTrue(rewrite.keywords().contains("香草风味咖啡粉"));
    }

    private static final class StubLlmGateway implements LLMGateway {

        private final RewriteResult rewriteResult;

        private StubLlmGateway(RewriteResult rewriteResult) {
            this.rewriteResult = rewriteResult;
        }

        @Override
        public RewriteResult rewriteTitle(String sourceTitle) {
            return rewriteResult;
        }

        @Override
        public ReportNarrativeResult generateReportNarrative(ReportNarrativeRequest request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ReasoningResult generateReasoning(ReasoningRequest request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public TranscriptIntentResult analyzeTranscript(TranscriptIntentRequest request) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class InMemoryQueryRewriteRepository implements QueryRewriteRepository {

        @Override
        public QueryRewrite save(QueryRewrite queryRewrite) {
            return queryRewrite;
        }

        @Override
        public Optional<QueryRewrite> findLatestBySourceText(String sourceText) {
            return Optional.empty();
        }

        @Override
        public Optional<QueryRewrite> findLatestByCandidateId(String candidateId) {
            return Optional.empty();
        }

        @Override
        public Optional<QueryRewrite> findLatestBySourceProductId(String sourceProductId) {
            return Optional.empty();
        }

        @Override
        public List<QueryRewrite> findByTaskId(String taskId) {
            return List.of();
        }
    }
}
