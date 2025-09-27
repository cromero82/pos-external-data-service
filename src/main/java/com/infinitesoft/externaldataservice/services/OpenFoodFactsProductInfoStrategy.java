package com.infinitesoft.externaldataservice.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infinitesoft.externaldataservice.dto.ProductInfoDTO;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class OpenFoodFactsProductInfoStrategy implements ProductInfoStrategy {
    private static final String API_URL = "https://world.openfoodfacts.org/api/v0/product/%s.json";

    @Override
    public Object getProductInfo(String barcode) {
        RestTemplate restTemplate = new RestTemplate();
        String url = String.format(API_URL, barcode);
        Object response = restTemplate.getForObject(url, Object.class);
        if (response instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) response;
            Object code = map.get("code");
            Object product = map.get("product");
            if (code != null && product instanceof Map) {
                ObjectMapper mapper = new ObjectMapper();
                ProductInfoDTO.Product productDto = mapper.convertValue(product, ProductInfoDTO.Product.class);
                return new ProductInfoDTO(code.toString(), productDto);
            }
        }
        return null;
    }
}