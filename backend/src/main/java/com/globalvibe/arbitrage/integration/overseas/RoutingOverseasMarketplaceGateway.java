package com.globalvibe.arbitrage.integration.overseas;

import com.globalvibe.arbitrage.config.IntegrationGatewayProperties;
import com.globalvibe.arbitrage.domain.product.model.Product;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Primary
public class RoutingOverseasMarketplaceGateway implements OverseasMarketplaceGateway {

    private final IntegrationGatewayProperties integrationGatewayProperties;
    private final HttpAmazonMarketplaceGateway httpAmazonMarketplaceGateway;
    private final SimulatedOverseasMarketplaceGateway simulatedOverseasMarketplaceGateway;

    public RoutingOverseasMarketplaceGateway(
            IntegrationGatewayProperties integrationGatewayProperties,
            HttpAmazonMarketplaceGateway httpAmazonMarketplaceGateway,
            SimulatedOverseasMarketplaceGateway simulatedOverseasMarketplaceGateway
    ) {
        this.integrationGatewayProperties = integrationGatewayProperties;
        this.httpAmazonMarketplaceGateway = httpAmazonMarketplaceGateway;
        this.simulatedOverseasMarketplaceGateway = simulatedOverseasMarketplaceGateway;
    }

    @Override
    public List<Product> searchProducts(String keyword, int limit) {
        if (!integrationGatewayProperties.getOverseas().isEnabled()) {
            return simulatedOverseasMarketplaceGateway.searchProducts(keyword, limit);
        }
        try {
            return httpAmazonMarketplaceGateway.searchProducts(keyword, limit);
        } catch (RuntimeException ex) {
            throw new IllegalStateException("海外实时商品抓取失败，将转入数据库快照兜底: " + ex.getMessage(), ex);
        }
    }
}
