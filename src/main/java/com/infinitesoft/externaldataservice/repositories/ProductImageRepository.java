package com.infinitesoft.externaldataservice.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;
import com.infinitesoft.externaldataservice.models.ProductImage;

public interface ProductImageRepository extends MongoRepository<ProductImage, String> {
    // Use default findById
}