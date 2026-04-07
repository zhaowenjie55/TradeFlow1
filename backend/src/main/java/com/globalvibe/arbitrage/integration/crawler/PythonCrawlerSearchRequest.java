package com.globalvibe.arbitrage.integration.crawler;

public record PythonCrawlerSearchRequest(
        String keyword,
        int page
) {
}
