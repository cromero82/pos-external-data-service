package com.infinitesoft.externaldataservice.services;

import org.springframework.web.multipart.MultipartFile;

public interface IImageService {
    Object uploadImage(String productId, MultipartFile file, String name, Integer expiration) throws Exception;
    Object getImageInfo(String productId);
}