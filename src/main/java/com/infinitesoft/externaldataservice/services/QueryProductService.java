package com.infinitesoft.externaldataservice.services;

import com.infinitesoft.externaldataservice.dto.ProductDTO;
import com.infinitesoft.externaldataservice.models.Product;
import com.infinitesoft.externaldataservice.repositories.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class QueryProductService {

    @Autowired
    private ProductRepository productRepository;

    public void updateProductPhoto(String productId, String photoUrl) {
        Product product = productRepository.findById(productId).orElse(null);
        if (product != null) {
            product.setPhotoUrl(photoUrl);
            productRepository.save(product);
        }
    }
}