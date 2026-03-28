package com.globalvibe.arbitrage.domain.product.controller;

import com.globalvibe.arbitrage.common.api.ApiResponse;
import com.globalvibe.arbitrage.domain.product.dto.ProductDetailVO;
import com.globalvibe.arbitrage.domain.product.service.ProductQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/product")
public class ProductController {

    private final ProductQueryService productQueryService;

    public ProductController(ProductQueryService productQueryService) {
        this.productQueryService = productQueryService;
    }

    @GetMapping("/{productId}")
    public ApiResponse<ProductDetailVO> detail(@PathVariable String productId) {
        return ApiResponse.success(productQueryService.getProduct(productId));
    }
}
