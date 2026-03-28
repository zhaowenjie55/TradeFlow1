package com.globalvibe.arbitrage.domain.search.repository;

import com.globalvibe.arbitrage.domain.product.model.Product;
import com.globalvibe.arbitrage.domain.search.model.SearchRun;
import com.globalvibe.arbitrage.domain.search.model.SearchRunResult;

import java.util.List;
import java.util.Optional;

public interface SearchRunRepository {

    SearchRun save(SearchRun searchRun);

    void replaceResults(String searchRunId, List<SearchRunResult> results);

    List<SearchRunResult> findResultsBySearchRunId(String searchRunId);

    Optional<SearchRun> findLatestSuccessfulRun(String platform, String queryText);

    default List<Product> findLatestProductsByQuery(String platform, String queryText, int limit) {
        return findLatestSuccessfulRun(platform, queryText)
                .map(run -> findResultsBySearchRunId(run.searchRunId()).stream()
                        .limit(limit)
                        .map(result -> new Product(
                                result.externalItemId(),
                                com.globalvibe.arbitrage.domain.marketplace.model.MarketplaceType.fromValue(result.platform()),
                                result.title(),
                                result.price(),
                                result.image(),
                                result.link(),
                                null,
                                null,
                                java.util.Map.of(),
                                result.rawData()
                        ))
                        .toList())
                .orElse(List.of());
    }
}
