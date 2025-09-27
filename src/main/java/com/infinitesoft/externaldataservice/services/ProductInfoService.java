package com.infinitesoft.externaldataservice.services;

import com.infinitesoft.externaldataservice.dto.ProductInfoDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductInfoService {
    private final List<ProductInfoStrategy> strategies;

    @Autowired
    public ProductInfoService(List<ProductInfoStrategy> strategies) {
        this.strategies = strategies;
    }

    public ProductInfoDTO getProductInfo(String barcode) {
        // Try each strategy in sequence until we get a successful result
        for (ProductInfoStrategy strategy : strategies) {
            try {
                Object result = strategy.getProductInfo(barcode);
                if (result instanceof ProductInfoDTO) {
                    ProductInfoDTO productInfo = (ProductInfoDTO) result;
                    if (productInfo != null && productInfo.getProduct() != null) {
                        return productInfo;
                    }
                }
            } catch (Exception e) {
                // Log the error but continue with the next strategy
                System.err.println("Error using strategy " + strategy.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }

        // If no strategy was successful, return null
        return null;
    }
}