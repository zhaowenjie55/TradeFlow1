package com.globalvibe.arbitrage.domain.product.service;

public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(String productId) {
        super("未找到商品: " + productId);
    }
}
