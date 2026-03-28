package com.globalvibe.arbitrage.integration.domestic;

import com.globalvibe.arbitrage.config.IntegrationGatewayProperties;
import com.globalvibe.arbitrage.domain.product.model.Product;
import com.globalvibe.arbitrage.domain.product.model.ProductDetailSnapshot;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@Primary
public class RoutingDomesticMarketplaceGateway implements DomesticMarketplaceGateway {

    private final IntegrationGatewayProperties integrationGatewayProperties;
    private final Http1688MarketplaceGateway http1688MarketplaceGateway;
    private final SimulatedDomesticMarketplaceGateway simulatedDomesticMarketplaceGateway;

    public RoutingDomesticMarketplaceGateway(
            IntegrationGatewayProperties integrationGatewayProperties,
            Http1688MarketplaceGateway http1688MarketplaceGateway,
            SimulatedDomesticMarketplaceGateway simulatedDomesticMarketplaceGateway
    ) {
        this.integrationGatewayProperties = integrationGatewayProperties;
        this.http1688MarketplaceGateway = http1688MarketplaceGateway;
        this.simulatedDomesticMarketplaceGateway = simulatedDomesticMarketplaceGateway;
    }

    @Override
    public List<Product> searchProducts(String keyword) {
        if (!integrationGatewayProperties.getDomestic().isEnabled()) {
            return simulatedDomesticMarketplaceGateway.searchProducts(keyword);
        }
        try {
            return http1688MarketplaceGateway.searchProducts(keyword);
        } catch (RuntimeException ex) {
            throw new IllegalStateException("国内实时货源搜索失败，将转入数据库货源兜底: " + ex.getMessage(), ex);
        }
    }

    @Override
    public Optional<ProductDetailSnapshot> loadDetail(String productId) {
        if (!integrationGatewayProperties.getDomestic().isEnabled()) {
            return simulatedDomesticMarketplaceGateway.loadDetail(productId);
        }
        try {
            return http1688MarketplaceGateway.loadDetail(productId);
        } catch (RuntimeException ex) {
            throw new IllegalStateException("国内详情抓取失败，将转入数据库详情兜底: " + ex.getMessage(), ex);
        }
    }
}
