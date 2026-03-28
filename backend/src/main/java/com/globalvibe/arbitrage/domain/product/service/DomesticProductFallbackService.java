package com.globalvibe.arbitrage.domain.product.service;

import com.globalvibe.arbitrage.domain.marketplace.model.MarketplaceType;
import com.globalvibe.arbitrage.domain.product.model.Product;
import com.globalvibe.arbitrage.domain.product.model.ProductDetailSnapshot;
import com.globalvibe.arbitrage.domain.product.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class DomesticProductFallbackService {

    private final ProductRepository productRepository;

    public DomesticProductFallbackService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<Product> searchHistoricalProducts(MarketplaceType marketplaceType, String keyword, int limit) {
        return productRepository.searchByPlatformAndKeyword(marketplaceType, keyword, limit);
    }

    public Optional<ProductDetailSnapshot> findHistoricalDetail(String productId) {
        return productRepository.findDetailByProductId(productId);
    }
}
