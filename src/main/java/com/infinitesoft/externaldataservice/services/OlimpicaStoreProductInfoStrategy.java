package com.infinitesoft.externaldataservice.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.infinitesoft.externaldataservice.dto.ProductInfoDTO;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

@Service
public class OlimpicaStoreProductInfoStrategy implements ProductInfoStrategy {

    @Override
    public Object getProductInfo(String barcode) {
        return getProductInfo(barcode, (Object[]) null);
    }

    public Object getProductInfo(String barcode, Object... extraParams) {
        try {
            String cookieHeader = null;
            if (extraParams != null && extraParams.length > 0 && extraParams[0] instanceof String) {
                cookieHeader = (String) extraParams[0];
            }

            String url = "https://www.olimpica.com/search?q=" + barcode;
            Document doc;
            
            if (cookieHeader != null && !cookieHeader.isEmpty()) {
                doc = Jsoup.connect(url)
                        .header("Cookie", cookieHeader)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                        .timeout(10000)
                        .get();
            } else {
                doc = Jsoup.connect(url)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                        .timeout(10000)
                        .get();
            }

            // First try to extract from state template
            ProductInfoDTO result = extractFromStateTemplate(doc, new ObjectMapper());
            if (result != null && result.getProduct() != null) {
                return result;
            }

            // If that fails, try to extract from script tags
            for (Element script : doc.select("script")) {
                if (script.html().contains("__PRELOADED_STATE__")) {
                    result = extractFromScriptTag(script, barcode, doc);
                    if (result != null && result.getProduct() != null) {
                        return result;
                    }
                }
            }

            // If all methods fail, return a fallback
            ProductInfoDTO dto = new ProductInfoDTO();
            dto.setCode(barcode);
            ProductInfoDTO.Product product = new ProductInfoDTO.Product();
            product.code = barcode;
            product.product_name = "Product not found";
            dto.setProduct(product);
            return dto;

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private ProductInfoDTO extractFromStateTemplate(Document doc, ObjectMapper mapper) {
        try {
            Element stateElement = doc.selectFirst("script#__NEXT_DATA__");
            if (stateElement != null) {
                String stateJson = stateElement.html();
                JsonNode rootNode = mapper.readTree(stateJson);
                
                JsonNode productsNode = rootNode.path("props").path("pageProps").path("searchResult").path("products");
                if (productsNode.isArray() && productsNode.size() > 0) {
                    JsonNode productNode = productsNode.get(0);
                    
                    ProductInfoDTO dto = new ProductInfoDTO();
                    dto.setCode(productNode.path("productReference").asText());
                    
                    ProductInfoDTO.Product product = new ProductInfoDTO.Product();
                    product.product_name = productNode.path("productName").asText();
                    product.brands = productNode.path("brand").asText();
                    product.link = "https://www.olimpica.com" + productNode.path("linkText").asText();
                    
                    // Get image URL
                    JsonNode imagesNode = productNode.path("items").get(0).path("images");
                    if (imagesNode.isArray() && imagesNode.size() > 0) {
                        product.image_url = imagesNode.get(0).path("imageUrl").asText();
                    }
                    
                    // Get pricing information
                    JsonNode sellerNode = productNode.path("items").get(0).path("sellers").get(0);
                    JsonNode offerNode = sellerNode.path("commertialOffer");
                    
                    double price = offerNode.path("Price").asDouble();
                    double listPrice = offerNode.path("ListPrice").asDouble();
                    
                    product.price = price;
                    dto.setPrice(price);
                    
                    if (listPrice > price) {
                        product.priceWithoutDiscount = listPrice;
                        dto.setPriceWithoutDiscount(listPrice);
                        
                        double discountPercent = ((listPrice - price) / listPrice) * 100;
                        product.percentDiscount = discountPercent;
                        dto.setPercentDiscount(discountPercent);
                    }
                    
                    // Try to parse price valid until date
                    String priceValidUntilStr = offerNode.path("PriceValidUntil").asText();
                    if (priceValidUntilStr != null && !priceValidUntilStr.isEmpty()) {
                        try {
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                            Date priceValidUntil = sdf.parse(priceValidUntilStr);
                            product.priceValidUntil = priceValidUntil;
                            dto.setPriceValidUntil(priceValidUntil);
                        } catch (ParseException e) {
                            // Ignore date parsing errors
                        }
                    }
                    
                    dto.setProduct(product);
                    return dto;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private ProductInfoDTO extractFromScriptTag(Element script, String barcode, Document doc) {
        try {
            String scriptContent = script.html();
            int startIndex = scriptContent.indexOf("__PRELOADED_STATE__");
            if (startIndex >= 0) {
                int jsonStartIndex = scriptContent.indexOf("{", startIndex);
                int jsonEndIndex = scriptContent.lastIndexOf("}") + 1;
                
                if (jsonStartIndex >= 0 && jsonEndIndex > jsonStartIndex) {
                    String jsonStr = scriptContent.substring(jsonStartIndex, jsonEndIndex);
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode rootNode = mapper.readTree(jsonStr);
                    
                    JsonNode searchResultNode = rootNode.path("searchResult");
                    JsonNode productsNode = searchResultNode.path("products");
                    
                    if (productsNode.isArray() && productsNode.size() > 0) {
                        for (JsonNode productNode : productsNode) {
                            String productReference = productNode.path("productReference").asText();
                            if (productReference.equals(barcode)) {
                                ProductInfoDTO dto = new ProductInfoDTO();
                                dto.setCode(barcode);
                                
                                ProductInfoDTO.Product product = new ProductInfoDTO.Product();
                                product.product_name = productNode.path("productName").asText();
                                product.brands = productNode.path("brand").asText();
                                product.link = "https://www.olimpica.com" + productNode.path("linkText").asText();
                                
                                // Get pricing information from the first seller
                                JsonNode itemsNode = productNode.path("items");
                                if (itemsNode.isArray() && itemsNode.size() > 0) {
                                    JsonNode sellersNode = itemsNode.get(0).path("sellers");
                                    if (sellersNode.isArray() && sellersNode.size() > 0) {
                                        JsonNode offerNode = sellersNode.get(0).path("commertialOffer");
                                        
                                        double price = offerNode.path("Price").asDouble();
                                        double listPrice = offerNode.path("ListPrice").asDouble();
                                        
                                        product.price = price;
                                        dto.setPrice(price);
                                        
                                        if (listPrice > price) {
                                            product.priceWithoutDiscount = listPrice;
                                            dto.setPriceWithoutDiscount(listPrice);
                                            
                                            double discountPercent = ((listPrice - price) / listPrice) * 100;
                                            product.percentDiscount = discountPercent;
                                            dto.setPercentDiscount(discountPercent);
                                        }
                                    }
                                }
                                
                                dto.setProduct(product);
                                return dto;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}