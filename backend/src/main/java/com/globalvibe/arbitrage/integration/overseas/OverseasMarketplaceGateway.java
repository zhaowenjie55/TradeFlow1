package com.globalvibe.arbitrage.integration.overseas;

import com.globalvibe.arbitrage.domain.product.model.Product;

import java.util.List;

public interface OverseasMarketplaceGateway {

    List<Product> searchProducts(String keyword, int limit);
}
