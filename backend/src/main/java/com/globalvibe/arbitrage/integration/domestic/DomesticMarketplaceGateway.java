package com.globalvibe.arbitrage.integration.domestic;

import com.globalvibe.arbitrage.domain.product.model.Product;
import com.globalvibe.arbitrage.domain.product.model.ProductDetailSnapshot;

import java.util.List;
import java.util.Optional;

public interface DomesticMarketplaceGateway {

    List<Product> searchProducts(String keyword);

    Optional<ProductDetailSnapshot> loadDetail(String productId);
}
