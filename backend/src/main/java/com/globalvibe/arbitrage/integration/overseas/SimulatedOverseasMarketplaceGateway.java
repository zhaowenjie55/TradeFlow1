package com.globalvibe.arbitrage.integration.overseas;

import com.globalvibe.arbitrage.domain.product.model.Product;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SimulatedOverseasMarketplaceGateway {

    public List<Product> searchProducts(String keyword, int limit) {
        throw new IllegalStateException(
                "黑客松模式下已跳过真实海外抓取，当前转入数据库历史搜索快照。"
        );
    }
}
