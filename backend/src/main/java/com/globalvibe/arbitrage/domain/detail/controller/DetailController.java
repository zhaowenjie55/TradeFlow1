package com.globalvibe.arbitrage.domain.detail.controller;

import com.globalvibe.arbitrage.domain.detail.dto.DetailRequest;
import com.globalvibe.arbitrage.domain.detail.dto.ProductDetailResponse;
import com.globalvibe.arbitrage.domain.detail.service.DetailService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/detail")
public class DetailController {

    private final DetailService detailService;

    public DetailController(DetailService detailService) {
        this.detailService = detailService;
    }

    @PostMapping("/amazon")
    public ProductDetailResponse amazonDetail(@Valid @RequestBody DetailRequest request) {
        return detailService.getAmazonDetail(request);
    }
}
