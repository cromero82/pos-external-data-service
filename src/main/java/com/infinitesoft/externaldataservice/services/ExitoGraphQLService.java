package com.infinitesoft.externaldataservice.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.infinitesoft.externaldataservice.config.ExitoApiConfig;
import com.infinitesoft.externaldataservice.dto.ProductInfoDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class ExitoGraphQLService {
    private static final Logger logger = Logger.getLogger(ExitoGraphQLService.class.getName());

    @Autowired
    private ExitoApiConfig apiConfig;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public ProductInfoDTO searchProductByBarcode(String barcode) {
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
                    "  \"query\": \"query QuerySearch($query: String, $map: String, $fullText: String, $selectedFacets: [SelectedFacetInput], $hideUnavailableItems: Boolean, $from: Int, $to: Int, $count: Int, $fuzzy: String, $operator: Operator, $searchState: String, $options: SearchOptionsInput, $simulationBehavior: SimulationBehavior) {\\n  search(\\n    query: $query\\n    map: $map\\n    fullText: $fullText\\n    selectedFacets: $selectedFacets\\n    hideUnavailableItems: $hideUnavailableItems\\n    from: $from\\n    to: $to\\n    count: $count\\n    fuzzy: $fuzzy\\n    operator: $operator\\n    searchState: $searchState\\n    options: $options\\n    simulationBehavior: $simulationBehavior\\n  ) {\\n    products {\\n      cacheId\\n      productId\\n      productName\\n      linkText\\n      brand\\n      brandId\\n      link\\n      description\\n      items {\\n        itemId\\n        name\\n        nameComplete\\n        complementName\\n        ean\\n        referenceId {\\n          Key\\n          Value\\n        }\\n        measurementUnit\\n        unitMultiplier\\n        modalType\\n        images {\\n          imageId\\n          imageLabel\\n          imageTag\\n          imageUrl\\n          imageText\\n        }\\n        videos {\\n          videoUrl\\n        }\\n        sellers {\\n          sellerId\\n          sellerName\\n          sellerDefault\\n          addToCartLink\\n          commertialOffer {\\n            discountHighlights {\\n              name\\n            }\\n            teasers {\\n              name\\n              conditions {\\n                minimumQuantity\\n                parameters {\\n                  name\\n                  value\\n                }\\n              }\\n              effects {\\n                parameters {\\n                  name\\n                  value\\n                }\\n              }\\n            }\\n            Price\\n            ListPrice\\n            PriceWithoutDiscount\\n            RewardValue\\n            PriceValidUntil\\n            AvailableQuantity\\n            Tax\\n            taxPercentage\\n            CacheVersionUsedToCallCheckout\\n          }\\n        }\\n        variations {\\n          name\\n          values\\n        }\\n      }\\n      productClusters {\\n        id\\n        name\\n      }\\n      properties {\\n        name\\n        values\\n      }\\n      origin\\n    }\\n    recordsFiltered\\n    correction {\\n      misspelled\\n      corrected\\n    }\\n    operator\\n    fuzzy\\n    translated\\n    pagination {\\n      count\\n      from\\n      to\\n      total\\n    }\\n  }\\n}\\n\"\n" +
                    "}";

            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.set("User-Agent", apiConfig.getUserAgent());
            headers.set("Cookie", apiConfig.getFormattedCookies());

            HttpEntity<String> entity = new HttpEntity<>(graphqlQuery, headers);
            RestTemplate restTemplate = new RestTemplate();

            ResponseEntity<String> response = restTemplate.exchange(
                    apiConfig.getFullGraphqlUrl(),
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            JsonNode rootNode = objectMapper.readTree(response.getBody());
            JsonNode searchNode = rootNode.path("data").path("search");
            JsonNode productsNode = searchNode.path("products");

            if (productsNode.isArray() && productsNode.size() > 0) {
                JsonNode product = productsNode.get(0);
                return buildProductInfoDTO(barcode, product);
            } else {
                logger.log(Level.INFO, "No products found for barcode: " + barcode);
                return null;
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error searching for product with barcode: " + barcode, e);
            return null;
        }
    }

    private ProductInfoDTO buildProductInfoDTO(String barcode, JsonNode product) {
        try {
            ProductInfoDTO dto = new ProductInfoDTO();
            dto.setCode(barcode);

            ProductInfoDTO.Product productObj = new ProductInfoDTO.Product();
            productObj.product_name = product.path("productName").asText();
            productObj.id = product.path("productId").asText();
            productObj.link = product.path("link").asText();
            productObj.description = product.path("description").asText();
            productObj.brands = product.path("brand").asText();

            // Get the first item
            JsonNode itemsNode = product.path("items");
            if (itemsNode.isArray() && itemsNode.size() > 0) {
                JsonNode item = itemsNode.get(0);
                
                // Set EAN code if available
                productObj.code = item.path("ean").asText();
                
                // Get images
                JsonNode imagesNode = item.path("images");
                if (imagesNode.isArray() && imagesNode.size() > 0) {
                    productObj.image_url = imagesNode.get(0).path("imageUrl").asText();
                }

                // Get sellers and pricing information
                JsonNode sellersNode = item.path("sellers");
                if (sellersNode.isArray() && sellersNode.size() > 0) {
                    List<ProductInfoDTO.Product.Seller> sellers = new ArrayList<>();
                    
                    for (JsonNode sellerNode : sellersNode) {
                        ProductInfoDTO.Product.Seller seller = new ProductInfoDTO.Product.Seller();
                        seller.sellerId = sellerNode.path("sellerId").asText();
                        seller.sellerName = sellerNode.path("sellerName").asText();
                        seller.sellerDefault = sellerNode.path("sellerDefault").asBoolean();
                        
                        JsonNode offerNode = sellerNode.path("commertialOffer");
                        if (!offerNode.isMissingNode()) {
                            ProductInfoDTO.Product.Seller.CommertialOffer offer = new ProductInfoDTO.Product.Seller.CommertialOffer();
                            offer.Price = offerNode.path("Price").asDouble();
                            offer.ListPrice = offerNode.path("ListPrice").asDouble();
                            offer.PriceWithoutDiscount = offerNode.path("PriceWithoutDiscount").asDouble();
                            offer.AvailableQuantity = offerNode.path("AvailableQuantity").asInt();
                            offer.Tax = offerNode.path("Tax").asDouble();
                            
                            seller.commertialOffer = offer;
                            
                            // If this is the default seller, use its pricing information for the product
                            if (seller.sellerDefault) {
                                productObj.price = offer.Price;
                                productObj.priceWithoutDiscount = offer.PriceWithoutDiscount;
                                
                                // Calculate discount percentage if applicable
                                if (offer.ListPrice > 0 && offer.Price < offer.ListPrice) {
                                    double discount = ((offer.ListPrice - offer.Price) / offer.ListPrice) * 100;
                                    productObj.percentDiscount = Math.round(discount * 100.0) / 100.0; // Round to 2 decimal places
                                }
                            }
                        }
                        
                        sellers.add(seller);
                    }
                    
                    productObj.sellers = sellers;
                }
            }

            // Extract product properties
            JsonNode propertiesNode = product.path("properties");
            if (propertiesNode.isArray()) {
                for (JsonNode property : propertiesNode) {
                    String name = property.path("name").asText();
                    JsonNode valuesNode = property.path("values");
                    
                    if (valuesNode.isArray() && valuesNode.size() > 0) {
                        String value = valuesNode.get(0).asText();
                        
                        switch (name.toLowerCase()) {
                            case "category":
                                productObj.category = value;
                                break;
                            case "subcategory":
                                productObj.subcategory = value;
                                break;
                            case "department":
                                productObj.department = value;
                                break;
                        }
                    }
                }
            }

            dto.setProduct(productObj);
            
            // Copy pricing information to the top level
            if (productObj.price != null) {
                dto.setPrice(productObj.price);
            }
            if (productObj.priceWithoutDiscount != null) {
                dto.setPriceWithoutDiscount(productObj.priceWithoutDiscount);
            }
            if (productObj.percentDiscount != null) {
                dto.setPercentDiscount(productObj.percentDiscount);
            }
            if (productObj.priceValidUntil != null) {
                dto.setPriceValidUntil(productObj.priceValidUntil);
            }
            
            return dto;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error building ProductInfoDTO from GraphQL response", e);
            return null;
        }
    }

    public void updateApiConfig(String rtbhLid, String gclLs, String spid) {
        if (rtbhLid != null && !rtbhLid.isEmpty()) {
            apiConfig.setRtbhLid(rtbhLid);
        }
        if (gclLs != null && !gclLs.isEmpty()) {
            apiConfig.setGclLs(gclLs);
        }
        if (spid != null && !spid.isEmpty()) {
            apiConfig.setSpid(spid);
        }
    }
}