package com.globalvibe.arbitrage.domain.search.service;

import com.globalvibe.arbitrage.domain.search.dto.QueryRewriteVO;
import com.globalvibe.arbitrage.domain.search.model.QueryRewrite;
import org.springframework.stereotype.Component;

@Component
public class QueryRewriteViewAssembler {

    public QueryRewriteVO toView(QueryRewrite queryRewrite) {
        return new QueryRewriteVO(
                queryRewrite.rewriteId(),
                queryRewrite.taskId(),
                queryRewrite.candidateId(),
                queryRewrite.sourceProductId(),
                queryRewrite.sourceText(),
                queryRewrite.rewrittenText(),
                queryRewrite.keywords(),
                queryRewrite.gatewaySource(),
                queryRewrite.fallbackUsed(),
                queryRewrite.fallbackReason(),
                queryRewrite.createdAt()
        );
    }
}
