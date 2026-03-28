package com.globalvibe.arbitrage.domain.product.service;

import com.globalvibe.arbitrage.domain.product.dto.ProductDetailVO;
import com.globalvibe.arbitrage.domain.product.model.Product;
import com.globalvibe.arbitrage.domain.product.model.ProductDetailSnapshot;
import com.globalvibe.arbitrage.domain.product.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ProductQueryService {

    private final ProductRepository productRepository;
    private final ProductCatalogSyncService productCatalogSyncService;
    private final ProductViewAssembler productViewAssembler;

    public ProductQueryService(
            ProductRepository productRepository,
            ProductCatalogSyncService productCatalogSyncService,
            ProductViewAssembler productViewAssembler
    ) {
        this.productRepository = productRepository;
        this.productCatalogSyncService = productCatalogSyncService;
        this.productViewAssembler = productViewAssembler;
    }

    public ProductDetailVO getProduct(String productId) {
        Optional<Product> product = productRepository.findById(productId);
        Optional<ProductDetailSnapshot> detailSnapshot = productRepository.findDetailByProductId(productId);

        if (detailSnapshot.isEmpty()) {
            detailSnapshot = productCatalogSyncService.syncTaobaoDetail(productId);
        }

        if (product.isEmpty() && detailSnapshot.isPresent()) {
            product = Optional.of(productCatalogSyncService.ensureBaseProduct(detailSnapshot.get()));
        }

        Product resolvedProduct = product.orElseThrow(() -> new ProductNotFoundException(productId));
        return productViewAssembler.toDetail(resolvedProduct, detailSnapshot.orElse(null));
    }
}
