package com.infinitesoft.externaldataservice.controllers.external;

import com.infinitesoft.externaldataservice.dto.ProductInfoDTO;
import com.infinitesoft.externaldataservice.services.OlimpicaStoreProductInfoStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/product-info/olimpica")
public class OlimpicaStoreProductInfoController {
    @Autowired
    private OlimpicaStoreProductInfoStrategy olimpicaStrategy;

    @GetMapping("/barcode/{barcode}")
    public ProductInfoDTO getOlimpicaProductInfo(
            @PathVariable String barcode,
            @RequestHeader(value = "Cookie", required = false) String cookieHeader) {
        Object result = olimpicaStrategy.getProductInfo(barcode, cookieHeader);
        if (result instanceof ProductInfoDTO) {
            return (ProductInfoDTO) result;
        }
        return null;
    }
}