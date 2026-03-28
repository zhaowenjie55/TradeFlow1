package com.globalvibe.arbitrage.domain.product.repository;

import com.globalvibe.arbitrage.domain.marketplace.model.MarketplaceType;
import com.globalvibe.arbitrage.domain.product.model.Product;
import com.globalvibe.arbitrage.domain.product.model.ProductDetailSnapshot;

import java.util.List;
import java.util.Optional;

public interface ProductRepository {

    void saveAll(List<Product> products);

    Product save(Product product);

    Optional<Product> findById(String productId);

    List<Product> findByPlatform(MarketplaceType platform);

    List<Product> searchByPlatformAndKeyword(MarketplaceType platform, String keyword, int limit);

    List<Product> searchByPlatformAndKeywordIncludingDetails(MarketplaceType platform, String keyword, int limit);

    List<Product> findAll();

    void saveDetail(ProductDetailSnapshot detailSnapshot);

    Optional<ProductDetailSnapshot> findDetailByProductId(String productId);
}
