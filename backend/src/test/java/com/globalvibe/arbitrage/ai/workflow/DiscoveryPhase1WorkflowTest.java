package com.globalvibe.arbitrage.ai.workflow;

import com.globalvibe.arbitrage.config.TaskExecutionProperties;
import com.globalvibe.arbitrage.domain.marketplace.model.MarketplaceType;
import com.globalvibe.arbitrage.domain.product.repository.ProductRepository;
import com.globalvibe.arbitrage.domain.search.service.AmazonCrawlerProductSearchService;
import com.globalvibe.arbitrage.domain.search.service.SearchHistoryFallbackService;
import com.globalvibe.arbitrage.domain.task.model.AnalysisTask;
import com.globalvibe.arbitrage.domain.task.model.TaskMode;
import com.globalvibe.arbitrage.domain.task.model.TaskPhase;
import com.globalvibe.arbitrage.domain.task.model.TaskStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DiscoveryPhase1WorkflowTest {

    @Test
    void surfacesCrawlerDetailWhenLiveSearchFailsAndFallbacksMiss() {
        TaskExecutionProperties taskExecutionProperties = new TaskExecutionProperties();
        AmazonCrawlerProductSearchService amazonCrawlerProductSearchService = mock(AmazonCrawlerProductSearchService.class);
        ProductRepository productRepository = mock(ProductRepository.class);
        SearchHistoryFallbackService searchHistoryFallbackService = mock(SearchHistoryFallbackService.class);

        when(amazonCrawlerProductSearchService.search("coffee", 1))
                .thenThrow(new RuntimeException("500 Internal Server Error: \"{\"detail\":\"SERPAPI_KEY is not set\"}\""));
        when(productRepository.searchByPlatformAndKeyword(MarketplaceType.AMAZON, "coffee", 60))
                .thenReturn(List.of());
        when(searchHistoryFallbackService.findLatestAmazonProducts("coffee", 60))
                .thenReturn(List.of());

        DiscoveryPhase1Workflow workflow = new DiscoveryPhase1Workflow(
                taskExecutionProperties,
                amazonCrawlerProductSearchService,
                productRepository,
                searchHistoryFallbackService
        );

        AnalysisTask analysisTask = AnalysisTask.builder()
                .taskId("phase1-test")
                .phase(TaskPhase.PHASE1)
                .status(TaskStatus.RUNNING)
                .keyword("coffee")
                .market("AMAZON")
                .requestedLimit(20)
                .targetProfitMargin(new BigDecimal("0.25"))
                .mode(TaskMode.AUTO_FALLBACK)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        Phase1WorkflowFailedException ex = assertThrows(
                Phase1WorkflowFailedException.class,
                () -> workflow.run(analysisTask)
        );

        assertTrue(ex.getMessage().contains("SERPAPI_KEY is not set"));
        assertTrue(ex.logs().stream().anyMatch(log -> log.stage().equals("phase1.live-search.failed")
                && log.message().contains("SERPAPI_KEY is not set")));
    }
}
