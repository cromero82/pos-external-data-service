package com.infinitesoft.externaldataservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Date;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductInfoDTO {
    private String code;
    private Product product;
    private Date priceValidUntil;
    private Double price;
    private Double priceWithoutDiscount;
    private Double percentDiscount;

    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    public static class Product {
        public String _id;
        public String[] _keywords;
        public String brands;
        public String[] brands_tags;
        public String categories;
        public String[] categories_hierarchy;
        public String categories_lc;
        public String categories_old;
        public Object categories_properties;
        public String[] categories_tags;
        public Object category_properties;
        public String[] checkers_tags;
        public String[] ciqual_food_name_tags;
        public String[] cities_tags;
        public String code;
        public String countries;
        public String[] entry_dates_tags;
        public String expiration_date;
        public String food_groups;
        public String[] food_groups_tags;
        public String generic_name;
        public String generic_name_es;
        public String generic_name_fr;
        public String id;
        public String image_front_url;
        public String image_url;
        public Integer known_ingredients_n;
        public String labels;
        public String[] labels_hierarchy;
        public String labels_lc;
        public String labels_old;
        public String[] labels_tags;
        public String lang;
        public Object languages;
        public String[] last_image_dates_tags;
        public Long last_image_t;
        public String last_modified_by;
        public Long last_modified_t;
        public Long last_updated_t;
        public String lc;
        public String link;
        public String product_name;
        public String product_name_es;
        public String product_name_fr;
        public String product_quantity;
        public String product_quantity_unit;
        public String product_type;
        public String quantity;
        public String serving_quantity;
        public String serving_quantity_unit;
        public String serving_size;
        public String stores;
        public String unit;
        public String package_unit;
        public String department;
        public String category;
        public String subcategory;
        public java.util.List<Seller> sellers;
        public Date priceValidUntil;
        public Double price;
        public Double priceWithoutDiscount;
        public Double percentDiscount;

        public static class Seller {
            public String sellerId;
            public String sellerName;
            public boolean sellerDefault;
            public CommertialOffer commertialOffer;

            public static class CommertialOffer {
                public int AvailableQuantity;
                public double Price;
                public double PriceWithoutDiscount;
                public double ListPrice;
                public double Tax;
            }
        }
    }

    public ProductInfoDTO() {}

    public ProductInfoDTO(String code, Product product) {
        this.code = code;
        this.product = product;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public Date getPriceValidUntil() {
        return priceValidUntil;
    }

    public void setPriceValidUntil(Date priceValidUntil) {
        this.priceValidUntil = priceValidUntil;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Double getPriceWithoutDiscount() {
        return priceWithoutDiscount;
    }

    public void setPriceWithoutDiscount(Double priceWithoutDiscount) {
        this.priceWithoutDiscount = priceWithoutDiscount;
    }

    public Double getPercentDiscount() {
        return percentDiscount;
    }

    public void setPercentDiscount(Double percentDiscount) {
        this.percentDiscount = percentDiscount;
    }
}