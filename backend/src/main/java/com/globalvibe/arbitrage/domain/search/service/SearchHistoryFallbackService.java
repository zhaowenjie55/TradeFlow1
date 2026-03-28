package com.globalvibe.arbitrage.domain.search.service;

import com.globalvibe.arbitrage.domain.marketplace.model.MarketplaceType;
import com.globalvibe.arbitrage.domain.product.model.Product;
import com.globalvibe.arbitrage.domain.search.repository.SearchRunRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SearchHistoryFallbackService {

    private final SearchRunRepository searchRunRepository;

    public SearchHistoryFallbackService(SearchRunRepository searchRunRepository) {
        this.searchRunRepository = searchRunRepository;
    }

    public List<Product> findLatestAmazonProducts(String queryText, int limit) {
        return searchRunRepository.findLatestProductsByQuery(MarketplaceType.AMAZON.value(), queryText, limit);
    }
}
