package com.globalvibe.arbitrage.domain.search.dto;

import java.util.List;

public record QueryRewriteListResponse(
        List<QueryRewriteVO> items
) {
}
