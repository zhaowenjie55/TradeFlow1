package com.globalvibe.arbitrage.integration.domestic;

import com.globalvibe.arbitrage.config.IntegrationGatewayProperties;
import com.globalvibe.arbitrage.domain.product.model.Product;
import com.globalvibe.arbitrage.domain.product.model.ProductDetailSnapshot;
import com.globalvibe.arbitrage.integration.GatewayFallbackException;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@Primary
public class RoutingDomesticMarketplaceGateway implements DomesticMarketplaceGateway {

    private final IntegrationGatewayProperties integrationGatewayProperties;
    private final Http1688MarketplaceGateway http1688MarketplaceGateway;

    public RoutingDomesticMarketplaceGateway(
            IntegrationGatewayProperties integrationGatewayProperties,
            Http1688MarketplaceGateway http1688MarketplaceGateway
    ) {
        this.integrationGatewayProperties = integrationGatewayProperties;
        this.http1688MarketplaceGateway = http1688MarketplaceGateway;
    }

    @Override
    public List<Product> searchProducts(String keyword) {
        if (!integrationGatewayProperties.getDomestic().isEnabled()) {
            throw new GatewayFallbackException("国内实时货源网关未启用，直接进入历史 1688 商品兜底。");
        }
        if (integrationGatewayProperties.getDomestic().isForceFallback()) {
            throw new GatewayFallbackException("国内实时货源网关已开启强制兜底，直接进入历史 1688 商品搜索。");
        }
        try {
            return http1688MarketplaceGateway.searchProducts(keyword);
        } catch (RuntimeException ex) {
            throw new GatewayFallbackException("国内实时货源搜索失败，将转入数据库货源兜底: " + ex.getMessage(), ex);
        }
    }

    @Override
    public Optional<ProductDetailSnapshot> loadDetail(String productId) {
        if (!integrationGatewayProperties.getDomestic().isEnabled()) {
            throw new GatewayFallbackException("国内详情网关未启用，直接进入历史详情快照兜底。");
        }
        if (integrationGatewayProperties.getDomestic().isForceFallback()) {
            throw new GatewayFallbackException("国内详情网关已开启强制兜底，直接进入历史详情快照。");
        }
        try {
            return http1688MarketplaceGateway.loadDetail(productId);
        } catch (RuntimeException ex) {
            throw new GatewayFallbackException("国内详情抓取失败，将转入数据库详情兜底: " + ex.getMessage(), ex);
        }
    }
}
