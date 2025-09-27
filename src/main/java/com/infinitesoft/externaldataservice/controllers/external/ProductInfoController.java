package com.infinitesoft.externaldataservice.controllers.external;

import com.infinitesoft.externaldataservice.dto.ProductInfoDTO;
import com.infinitesoft.externaldataservice.services.ExitoProductInfoStrategy;
import com.infinitesoft.externaldataservice.services.OpenFoodFactsProductInfoStrategy;
import com.infinitesoft.externaldataservice.services.ProductInfoService;
import com.infinitesoft.externaldataservice.services.ExitoGraphQLService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/product-info")
public class ProductInfoController {
    @Autowired
    private ProductInfoService productInfoService;

    @Autowired
    private ExitoProductInfoStrategy exitoStrategy;

    @Autowired
    private OpenFoodFactsProductInfoStrategy openFoodFactsStrategy;

    @Autowired
    private ExitoGraphQLService exitoGraphQLService;

    @GetMapping("/barcode/{barcode}")
    public ProductInfoDTO getProductInfoByBarcode(@PathVariable String barcode) {
        return productInfoService.getProductInfo(barcode);
    }

    @GetMapping("/exito/barcode/{barcode}")
    public ProductInfoDTO getExitoProductInfo(@PathVariable String barcode) {
        Object result = exitoStrategy.getProductInfo(barcode);
        if (result instanceof ProductInfoDTO) {
            return (ProductInfoDTO) result;
        }
        return null;
    }

    @GetMapping("/openfoodfacts/barcode/{barcode}")
    public ProductInfoDTO getOpenFoodFactsProductInfo(@PathVariable String barcode) {
        Object result = openFoodFactsStrategy.getProductInfo(barcode);
        if (result instanceof ProductInfoDTO) {
            return (ProductInfoDTO) result;
        }
        return null;
    }

    @GetMapping("/exito/graphql/{barcode}")
    public ProductInfoDTO getExitoGraphQLProductInfo(
            @PathVariable String barcode,
            @RequestParam(required = false) String rtbhLid,
            @RequestParam(required = false) String gclLs,
            @RequestParam(required = false) String spid) {

        // Update the configuration if parameters are provided
        if (rtbhLid != null || gclLs != null || spid != null) {
            exitoGraphQLService.updateApiConfig(rtbhLid, gclLs, spid);
        }

        // Get product information using the GraphQL service
        return exitoGraphQLService.searchProductByBarcode(barcode);
    }

    /**
     * POST endpoint for the GraphQL service that accepts parameters in the request body
     */
    @PostMapping("/exito/graphql/{barcode}")
    public ProductInfoDTO postExitoGraphQLProductInfo(
            @PathVariable String barcode,
            @RequestBody(required = false) ExitoApiRequestBody requestBody) {

        if (requestBody != null) {
            // Update the configuration if parameters are provided in the request body
            exitoGraphQLService.updateApiConfig(
                    requestBody.getRtbhLid(),
                    requestBody.getGclLs(),
                    requestBody.getSpid()
            );
        }

        // Get product information using the GraphQL service
        return exitoGraphQLService.searchProductByBarcode(barcode);
    }

    /**
     * Request body class for POST requests to the GraphQL service
     */
    public static class ExitoApiRequestBody {
        private String rtbhLid;
        private String gclLs;
        private String spid;

        public String getRtbhLid() {
            return rtbhLid;
        }

        public void setRtbhLid(String rtbhLid) {
            this.rtbhLid = rtbhLid;
        }

        public String getGclLs() {
            return gclLs;
        }

        public void setGclLs(String gclLs) {
            this.gclLs = gclLs;
        }

        public String getSpid() {
            return spid;
        }

        public void setSpid(String spid) {
            this.spid = spid;
        }
    }
}