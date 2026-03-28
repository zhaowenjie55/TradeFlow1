package com.globalvibe.arbitrage.integration.overseas;

import com.globalvibe.arbitrage.config.IntegrationGatewayProperties;
import com.globalvibe.arbitrage.domain.product.model.Product;
import com.globalvibe.arbitrage.integration.GatewayFallbackException;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Primary
public class RoutingOverseasMarketplaceGateway implements OverseasMarketplaceGateway {

    private final IntegrationGatewayProperties integrationGatewayProperties;
    private final HttpAmazonMarketplaceGateway httpAmazonMarketplaceGateway;

    public RoutingOverseasMarketplaceGateway(
            IntegrationGatewayProperties integrationGatewayProperties,
            HttpAmazonMarketplaceGateway httpAmazonMarketplaceGateway
    ) {
        this.integrationGatewayProperties = integrationGatewayProperties;
        this.httpAmazonMarketplaceGateway = httpAmazonMarketplaceGateway;
    }

    @Override
    public List<Product> searchProducts(String keyword, int limit) {
        if (!integrationGatewayProperties.getOverseas().isEnabled()) {
            throw new GatewayFallbackException("海外实时商品网关未启用，直接进入历史 Amazon 搜索快照兜底。");
        }
        if (integrationGatewayProperties.getOverseas().isForceFallback()) {
            throw new GatewayFallbackException("海外实时商品网关已开启强制兜底，直接进入历史 Amazon 搜索快照。");
        }
        try {
            return httpAmazonMarketplaceGateway.searchProducts(keyword, limit);
        } catch (RuntimeException ex) {
            throw new GatewayFallbackException("海外实时商品抓取失败，将转入数据库快照兜底: " + ex.getMessage(), ex);
        }
    }
}
