package com.globalvibe.arbitrage.domain.search.service;

import com.globalvibe.arbitrage.config.VectorSearchProperties;
import com.globalvibe.arbitrage.domain.search.model.QueryRewrite;
import com.globalvibe.arbitrage.domain.search.repository.QueryRewriteRepository;
import com.globalvibe.arbitrage.integration.llm.LLMGateway;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class QueryRewriteServiceTest {

    @Test
    void shouldAlwaysAppendFixedKeywordBeforePersisting() {
        LLMGateway llmGateway = mock(LLMGateway.class);
        QueryRewriteRepository repository = mock(QueryRewriteRepository.class);
        VectorSearchProperties properties = new VectorSearchProperties();
        properties.setFixedKeyword("亚克力透明收纳架");

        when(llmGateway.rewriteTitle("Acrylic Desktop Organizer")).thenReturn(new LLMGateway.RewriteResult(
                "桌面收纳架",
                List.of("桌面收纳架", "透明收纳架"),
                false,
                "GLM_CHAT",
                "glm-5",
                null,
                OffsetDateTime.now()
        ));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0, QueryRewrite.class));

        QueryRewriteService service = new QueryRewriteService(llmGateway, repository, properties);
        QueryRewriteService.RewriteExecutionResult result = service.rewrite(
                "phase2-1",
                "phase1-1:amz-acrylic-01",
                "amz-acrylic-01",
                "Acrylic Desktop Organizer"
        );

        assertTrue(result.queryRewrite().keywords().contains("亚克力透明收纳架"));
        assertTrue("glm-5".equals(result.queryRewrite().gatewayModel()));
    }
}
