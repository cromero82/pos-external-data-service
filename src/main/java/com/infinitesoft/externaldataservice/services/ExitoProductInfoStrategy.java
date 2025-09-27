package com.infinitesoft.externaldataservice.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.infinitesoft.externaldataservice.dto.ProductInfoDTO;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class ExitoProductInfoStrategy implements ProductInfoStrategy {
    private static final Logger logger = Logger.getLogger(ExitoProductInfoStrategy.class.getName());
    private static final String SEARCH_URL = "https://www.exito.com/search?_q=%s&map=ft";
    private static final String API_URL = "https://www.exito.com/api/catalog_system/pub/products/search?fq=ean:%s";
    private static final String GRAPHQL_URL = "https://www.exito.com/api/graphql?operationName=QuerySearch";

    @Override
    public Object getProductInfo(String barcode) {
        try {
            // First try the API
            ProductInfoDTO result = fetchProductInfoFromApi(String.format(API_URL, barcode), barcode);
            if (result != null && result.getProduct() != null) {
                return result;
            }

            // If API fails, try scraping the search page
            result = scrapeProductInfoFromSearchPage(barcode);
            if (result != null && result.getProduct() != null) {
                return result;
            }

            // If scraping fails, try GraphQL API
            result = fetchProductInfoFromGraphQL(barcode);
            if (result != null && result.getProduct() != null) {
                return result;
            }

            // If all methods fail, return a fallback
            return fallbackProductInfo(barcode);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error getting product info for barcode: " + barcode, e);
            return fallbackProductInfo(barcode);
        }
    }

    private ProductInfoDTO fetchProductInfoFromApi(String url, String barcode) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            String responseBody = response.getBody();

            if (responseBody != null && !responseBody.equals("[]")) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode rootNode = mapper.readTree(responseBody);

                if (rootNode.isArray() && rootNode.size() > 0) {
                    JsonNode productNode = rootNode.get(0);
                    
                    ProductInfoDTO dto = new ProductInfoDTO();
                    dto.setCode(barcode);
                    
                    ProductInfoDTO.Product product = new ProductInfoDTO.Product();
                    product.product_name = productNode.path("productName").asText();
                    product.brands = productNode.path("brand").asText();
                    product.link = productNode.path("link").asText();
                    product.image_url = productNode.path("items").get(0).path("images").get(0).path("imageUrl").asText();
                    
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
                    
                    dto.setProduct(product);
                    return dto;
                }
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error fetching product info from API for barcode: " + barcode, e);
        }
        return null;
    }

    private ProductInfoDTO scrapeProductInfoFromSearchPage(String barcode) {
        try {
            String url = String.format(SEARCH_URL, barcode);
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .timeout(10000)
                    .get();

            // Look for product information in the search results
            Elements productElements = doc.select(".vtex-search-result-3-x-galleryItem");
            if (!productElements.isEmpty()) {
                Element productElement = productElements.first();
                
                ProductInfoDTO dto = new ProductInfoDTO();
                dto.setCode(barcode);
                
                ProductInfoDTO.Product product = new ProductInfoDTO.Product();
                product.product_name = productElement.select(".vtex-product-summary-2-x-productNameContainer").text();
                
                // Get image URL
                Element imgElement = productElement.select(".vtex-product-summary-2-x-imageContainer img").first();
                if (imgElement != null) {
                    product.image_url = imgElement.attr("src");
                }
                
                // Get price information
                Element priceElement = productElement.select(".vtex-product-price-1-x-sellingPrice").first();
                if (priceElement != null) {
                    String priceText = priceElement.text().replace("$", "").replace(".", "").trim();
                    try {
                        double price = NumberFormat.getInstance(Locale.FRANCE).parse(priceText).doubleValue();
                        product.price = price;
                        dto.setPrice(price);
                    } catch (Exception e) {
                        logger.log(Level.WARNING, "Error parsing price: " + priceText, e);
                    }
                }
                
                // Get original price if available (for discount calculation)
                Element listPriceElement = productElement.select(".vtex-product-price-1-x-listPrice").first();
                if (listPriceElement != null) {
                    String listPriceText = listPriceElement.text().replace("$", "").replace(".", "").trim();
                    try {
                        double listPrice = NumberFormat.getInstance(Locale.FRANCE).parse(listPriceText).doubleValue();
                        product.priceWithoutDiscount = listPrice;
                        dto.setPriceWithoutDiscount(listPrice);
                        
                        if (product.price != null && product.price < listPrice) {
                            double discountPercent = ((listPrice - product.price) / listPrice) * 100;
                            product.percentDiscount = discountPercent;
                            dto.setPercentDiscount(discountPercent);
                        }
                    } catch (Exception e) {
                        logger.log(Level.WARNING, "Error parsing list price: " + listPriceText, e);
                    }
                }
                
                // Get product link
                Element linkElement = productElement.select("a.vtex-product-summary-2-x-clearLink").first();
                if (linkElement != null) {
                    product.link = "https://www.exito.com" + linkElement.attr("href");
                }
                
                dto.setProduct(product);
                return dto;
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error scraping product info for barcode: " + barcode, e);
        }
        return null;
    }

    private ProductInfoDTO fetchProductInfoFromGraphQL(String barcode) {
        try {
            String graphqlQuery = "{\n" +
                    "  \"operationName\": \"QuerySearch\",\n" +
                    "  \"variables\": {\n" +
                    "    \"hideUnavailableItems\": false,\n" +
                    "    \"simulationBehavior\": \"default\",\n" +
                    "    \"query\": \"" + barcode + "\",\n" +
                    "    \"fullText\": \"" + barcode + "\",\n" +
                    "    \"selectedFacets\": [],\n" +
                    "    \"from\": 0,\n" +
                    "    \"to\": 11,\n" +
                    "    \"count\": 1,\n" +
                    "    \"fuzzy\": \"0\",\n" +
                    "    \"operator\": \"and\",\n" +
                    "    \"searchState\": null,\n" +
                    "    \"options\": {\n" +
                    "      \"fetchMissing\": false\n" +
                    "    }\n" +
                    "  },\n" +
                    "  \"query\": \"query QuerySearch($query: String, $map: String, $fullText: String, $selectedFacets: [SelectedFacetInput], $hideUnavailableItems: Boolean, $from: Int, $to: Int, $count: Int, $fuzzy: String, $operator: Operator, $searchState: String, $options: SearchOptionsInput, $simulationBehavior: SimulationBehavior) {\\n  search(\\n    query: $query\\n    map: $map\\n    fullText: $fullText\\n    selectedFacets: $selectedFacets\\n    hideUnavailableItems: $hideUnavailableItems\\n    from: $from\\n    to: $to\\n    count: $count\\n    fuzzy: $fuzzy\\n    operator: $operator\\n    searchState: $searchState\\n    options: $options\\n    simulationBehavior: $simulationBehavior\\n  ) {\\n    products {\\n      productId\\n      productName\\n      linkText\\n      brand\\n      link\\n      items {\\n        itemId\\n        name\\n        ean\\n        images {\\n          imageUrl\\n        }\\n        sellers {\\n          sellerId\\n          sellerName\\n          sellerDefault\\n          commertialOffer {\\n            Price\\n            ListPrice\\n            PriceWithoutDiscount\\n            AvailableQuantity\\n          }\\n        }\\n      }\\n    }\\n    recordsFiltered\\n  }\\n}\"\n" +
                    "}";

            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");

            HttpEntity<String> entity = new HttpEntity<>(graphqlQuery, headers);
            RestTemplate restTemplate = new RestTemplate();

            ResponseEntity<String> response = restTemplate.exchange(
                    GRAPHQL_URL,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (isValidJson(response.getBody())) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode rootNode = mapper.readTree(response.getBody());
                JsonNode productsNode = rootNode.path("data").path("search").path("products");

                if (productsNode.isArray() && productsNode.size() > 0) {
                    JsonNode productNode = productsNode.get(0);
                    
                    ProductInfoDTO dto = new ProductInfoDTO();
                    dto.setCode(barcode);
                    
                    ProductInfoDTO.Product product = new ProductInfoDTO.Product();
                    product.product_name = productNode.path("productName").asText();
                    product.brands = productNode.path("brand").asText();
                    product.link = productNode.path("link").asText();
                    
                    JsonNode itemNode = productNode.path("items").get(0);
                    product.code = itemNode.path("ean").asText();
                    
                    if (itemNode.path("images").isArray() && itemNode.path("images").size() > 0) {
                        product.image_url = itemNode.path("images").get(0).path("imageUrl").asText();
                    }
                    
                    JsonNode sellerNode = itemNode.path("sellers").get(0);
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
                    
                    dto.setProduct(product);
                    return dto;
                }
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error fetching product info from GraphQL for barcode: " + barcode, e);
        }
        return null;
    }

    private boolean isValidJson(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.readTree(json);
            return true;
        } catch (JsonProcessingException e) {
            return false;
        }
    }

    private ProductInfoDTO fallbackProductInfo(String barcode) {
        ProductInfoDTO dto = new ProductInfoDTO();
        dto.setCode(barcode);
        ProductInfoDTO.Product product = new ProductInfoDTO.Product();
        product.code = barcode;
        product.product_name = "Product not found";
        dto.setProduct(product);
        return dto;
    }
}