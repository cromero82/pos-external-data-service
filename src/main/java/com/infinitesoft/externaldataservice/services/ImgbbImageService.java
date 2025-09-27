package com.infinitesoft.externaldataservice.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.infinitesoft.externaldataservice.models.ProductImage;
import com.infinitesoft.externaldataservice.repositories.ProductImageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;

@Service
public class ImgbbImageService implements IImageService {

    @Value("${imgbb.api.key}")
    private String apiKey;

    private static final String UPLOAD_URL = "https://api.imgbb.com/1/upload";

    @Autowired
    private ProductImageRepository productImageRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Object uploadImage(String productId, MultipartFile file, String name, Integer expiration) throws Exception {
        RestTemplate restTemplate = new RestTemplate();
        String base64Image = Base64.getEncoder().encodeToString(file.getBytes());

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("key", apiKey);
        body.add("image", base64Image);
        if (name != null) body.add("name", name);
        if (expiration != null) body.add("expiration", expiration.toString());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(UPLOAD_URL, requestEntity, String.class);
        JsonNode jsonNode = objectMapper.readTree(response.getBody());
        JsonNode dataNode = jsonNode.get("data");
        // Store in MongoDB
        ProductImage productImage = new ProductImage(productId, objectMapper.convertValue(dataNode, Object.class));
        productImageRepository.save(productImage);
        return dataNode;
    }

    @Override
    public Object getImageInfo(String productId) {
        ProductImage productImage = productImageRepository.findById(productId).orElse(null);
        if (productImage != null) {
            return productImage.getImgbb();
        }
        return null;
    }
}