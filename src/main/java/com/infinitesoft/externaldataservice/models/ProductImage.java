package com.infinitesoft.externaldataservice.models;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "products_image")
public class ProductImage {
    @Id
    private String productId;
    private Object imgbb;

    public ProductImage() {}
    public ProductImage(String productId, Object imgbb) {
        this.productId = productId;
        this.imgbb = imgbb;
    }
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public Object getImgbb() { return imgbb; }
    public void setImgbb(Object imgbb) { this.imgbb = imgbb; }
}