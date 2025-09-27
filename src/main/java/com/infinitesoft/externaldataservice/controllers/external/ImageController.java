package com.infinitesoft.externaldataservice.controllers.external;

import com.infinitesoft.externaldataservice.services.IImageService;
import com.infinitesoft.externaldataservice.services.QueryProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URL;

@RestController
@RequestMapping("/api/images")
public class ImageController {
    @Autowired
    private IImageService imageService;

    @Autowired
    private QueryProductService queryProductService;

    public void updateProductPhotoFromImage(String productId) {
        Object imageObj = imageService.getImageInfo(productId);
        if (imageObj instanceof java.util.Map) {
            Object urlObj = ((java.util.Map<?, ?>) imageObj).get("url");
            if (urlObj != null) {
                queryProductService.updateProductPhoto(productId, urlObj.toString());
            }
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadImage(
            @RequestParam("productId") String productId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "expiration", required = false) Integer expiration
    ) {
        try {
            Object response = imageService.uploadImage(productId, file, name, expiration);
            updateProductPhotoFromImage(productId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<?> getImageInfo(@PathVariable String productId) {
        Object info = imageService.getImageInfo(productId);
        if (info != null) {
            return ResponseEntity.ok(info);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/product/{productId}/file")
    public ResponseEntity<?> getImageFile(@PathVariable String productId) {
        Object info = imageService.getImageInfo(productId);
        if (info == null) {
            return ResponseEntity.notFound().build();
        }
        try {
            // Extract display_url from imgbb info
            String displayUrl = ((java.util.Map<?, ?>) info).get("display_url").toString();
            URL url = new URL(displayUrl);
            InputStream inputStream = url.openStream();
            InputStreamResource resource = new InputStreamResource(inputStream);
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=image.jpg");
            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(MediaType.IMAGE_JPEG)
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Could not fetch image: " + e.getMessage());
        }
    }
}