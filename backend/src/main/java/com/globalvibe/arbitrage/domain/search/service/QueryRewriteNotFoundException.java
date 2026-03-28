package com.globalvibe.arbitrage.domain.search.service;

public class QueryRewriteNotFoundException extends RuntimeException {

    public QueryRewriteNotFoundException(String candidateOrProductId) {
        super("未找到 candidateId/sourceProductId=%s 对应的改写记录。".formatted(candidateOrProductId));
    }
}
