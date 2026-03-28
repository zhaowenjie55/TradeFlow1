package com.globalvibe.arbitrage.domain.product.service;

import com.globalvibe.arbitrage.domain.marketplace.model.MarketplaceType;
import com.globalvibe.arbitrage.domain.product.model.Product;
import com.globalvibe.arbitrage.domain.product.model.ProductDetailSnapshot;
import com.globalvibe.arbitrage.domain.product.repository.ProductRepository;
import com.globalvibe.arbitrage.integration.domestic.DomesticMarketplaceGateway;
import com.globalvibe.arbitrage.integration.overseas.OverseasMarketplaceGateway;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ProductCatalogSyncService {

    private final ProductRepository productRepository;
    private final DomesticMarketplaceGateway domesticMarketplaceGateway;
    private final OverseasMarketplaceGateway overseasMarketplaceGateway;

    public ProductCatalogSyncService(
            ProductRepository productRepository,
            DomesticMarketplaceGateway domesticMarketplaceGateway,
            OverseasMarketplaceGateway overseasMarketplaceGateway
    ) {
        this.productRepository = productRepository;
        this.domesticMarketplaceGateway = domesticMarketplaceGateway;
        this.overseasMarketplaceGateway = overseasMarketplaceGateway;
    }

    public List<Product> syncDomesticKeywordProducts(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        List<Product> products = domesticMarketplaceGateway.searchProducts(keyword);
        if (!products.isEmpty()) {
            productRepository.saveAll(products);
        }
        return products;
    }

    public List<Product> syncAmazonProducts(String keyword, int limit) {
        if (limit <= 0) {
            return List.of();
        }
        List<Product> products = overseasMarketplaceGateway.searchProducts(keyword, limit);
        if (!products.isEmpty()) {
            productRepository.saveAll(products);
        }
        return products;
    }

    public Optional<ProductDetailSnapshot> syncTaobaoDetail(String productId) {
        Optional<ProductDetailSnapshot> detailSnapshot = domesticMarketplaceGateway.loadDetail(productId);
        detailSnapshot.ifPresent(detail -> {
            productRepository.saveDetail(detail);
            ensureBaseProduct(detail);
        });
        return detailSnapshot;
    }

    public Product ensureBaseProduct(ProductDetailSnapshot detailSnapshot) {
        Map<String, Object> attributes = detailSnapshot.brand() == null || detailSnapshot.brand().isBlank()
                ? Map.of()
                : Map.of("brand", detailSnapshot.brand());
        return productRepository.findById(detailSnapshot.productId())
                .orElseGet(() -> productRepository.save(new Product(
                        detailSnapshot.productId(),
                        MarketplaceType.TAOBAO,
                        detailSnapshot.title(),
                        detailSnapshot.price(),
                        detailSnapshot.image(),
                        detailSnapshot.link(),
                        null,
                        null,
                        attributes,
                        detailSnapshot.rawData()
                )));
    }
}
