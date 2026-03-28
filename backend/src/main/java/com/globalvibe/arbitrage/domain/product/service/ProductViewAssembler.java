package com.globalvibe.arbitrage.domain.product.service;

import com.globalvibe.arbitrage.domain.product.dto.ProductDetailVO;
import com.globalvibe.arbitrage.domain.product.model.Product;
import com.globalvibe.arbitrage.domain.product.model.ProductDetailSnapshot;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class ProductViewAssembler {

    public ProductDetailVO toDetail(Product product, ProductDetailSnapshot detailSnapshot) {
        return new ProductDetailVO(
                product.id(),
                product.platform().value(),
                detailSnapshot != null ? detailSnapshot.title() : product.title(),
                detailSnapshot != null ? detailSnapshot.price() : product.price(),
                detailSnapshot != null ? detailSnapshot.image() : product.image(),
                detailSnapshot != null ? detailSnapshot.link() : product.link(),
                product.rating(),
                product.reviews(),
                detailSnapshot != null ? detailSnapshot.attributes() : product.attributes(),
                detailSnapshot != null ? detailSnapshot.brand() : stringValue(product.attributes().get("brand")),
                detailSnapshot != null ? detailSnapshot.description() : null,
                detailSnapshot != null ? detailSnapshot.gallery() : List.of(),
                detailSnapshot != null ? detailSnapshot.skuData() : Map.of(),
                detailSnapshot != null
        );
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
