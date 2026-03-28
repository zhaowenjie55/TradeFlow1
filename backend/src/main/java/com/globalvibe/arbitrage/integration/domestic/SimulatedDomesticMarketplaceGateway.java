package com.globalvibe.arbitrage.integration.domestic;

import com.globalvibe.arbitrage.domain.product.model.Product;
import com.globalvibe.arbitrage.domain.product.model.ProductDetailSnapshot;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class SimulatedDomesticMarketplaceGateway {

    public List<Product> searchProducts(String keyword) {
        throw new IllegalStateException(
                "黑客松模式下已跳过真实国内平台抓取，当前转入数据库历史货源数据。"
        );
    }

    public Optional<ProductDetailSnapshot> loadDetail(String productId) {
        throw new IllegalStateException(
                "黑客松模式下已跳过真实详情抓取，当前转入数据库历史详情快照。"
        );
    }
}
