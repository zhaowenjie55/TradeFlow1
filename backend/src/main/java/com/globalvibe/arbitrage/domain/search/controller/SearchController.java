package com.globalvibe.arbitrage.domain.search.controller;

import com.globalvibe.arbitrage.domain.search.dto.SearchRequest;
import com.globalvibe.arbitrage.domain.search.dto.SearchResponse;
import com.globalvibe.arbitrage.domain.search.service.SearchService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public API entrypoint for Amazon keyword search.
 * <p>
 * This controller keeps HTTP concerns thin and delegates orchestration and
 * response mapping to {@link SearchService}. The downstream call path is:
 * Spring Boot -> Python FastAPI crawler -> SerpApi.
 */
@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    /**
     * Searches Amazon products through the Python crawler service.
     */
    @PostMapping("/amazon")
    public SearchResponse searchAmazon(@Valid @RequestBody SearchRequest request) {
        return searchService.searchAmazon(request);
    }
}
