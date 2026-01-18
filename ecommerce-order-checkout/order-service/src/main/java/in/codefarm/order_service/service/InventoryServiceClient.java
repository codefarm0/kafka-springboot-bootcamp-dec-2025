package in.codefarm.order_service.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Client service to interact with Inventory Service API.
 * Fetches products from inventory service for order placement.
 */
@Service
@Slf4j
public class InventoryServiceClient {
    
    private final RestTemplate restTemplate;
    private final String inventoryServiceUrl;
    
    public InventoryServiceClient(
            RestTemplate restTemplate,
            @Value("${inventory.service.url:http://localhost:8082}") String inventoryServiceUrl) {
        this.restTemplate = restTemplate;
        this.inventoryServiceUrl = inventoryServiceUrl;
    }
    
    /**
     * Get all products from inventory service.
     * Returns empty list if inventory service is unavailable.
     */
    public List<Product> getAllProducts() {
        try {
            String url = inventoryServiceUrl + "/api/products";
            log.info("Fetching products from inventory service: {}", url);
            
            ResponseEntity<List<ProductResponse>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<ProductResponse>>() {}
            );
            
            List<Product> products = response.getBody().stream()
                .map(p -> new Product(
                    p.getProductId(),
                    p.getProductName(),
                    p.getPrice().doubleValue()
                ))
                .collect(Collectors.toList());
            
            log.info("Fetched {} products from inventory service", products.size());
            return products;
            
        } catch (RestClientException e) {
            log.warn("Failed to fetch products from inventory service: {}. Using empty list.", e.getMessage());
            return Collections.emptyList();
        }
    }
    
    /**
     * Add a product to inventory service.
     */
    public Product addProduct(String productId, String productName, String description, 
                             BigDecimal price, Integer quantity) {
        try {
            String url = inventoryServiceUrl + "/api/products";
            log.info("Adding product to inventory service: productId={}", productId);
            
            ProductController.AddProductRequest request = new ProductController.AddProductRequest(
                productId, productName, description, price, quantity
            );
            
            ProductResponse response = restTemplate.postForObject(url, request, ProductResponse.class);
            
            if (response != null) {
                log.info("Successfully added product: productId={}", productId);
                return new Product(response.getProductId(), response.getProductName(), 
                    response.getPrice().doubleValue());
            }
            
            throw new RuntimeException("Failed to add product: null response");
            
        } catch (RestClientException e) {
            log.error("Failed to add product to inventory service: productId={}", productId, e);
            throw new RuntimeException("Failed to add product: " + e.getMessage(), e);
        }
    }
    
    /**
     * Product data class for order service.
     */
    public static class Product {
        private String productId;
        private String productName;
        private double price;
        
        public Product(String productId, String productName, double price) {
            this.productId = productId;
            this.productName = productName;
            this.price = price;
        }
        
        public String getProductId() { return productId; }
        public String getProductName() { return productName; }
        public double getPrice() { return price; }
    }
    
    /**
     * Product response from inventory service.
     */
    private static class ProductResponse {
        private String productId;
        private String productName;
        private String description;
        private BigDecimal price;
        private Integer availableQuantity;
        private Integer reservedQuantity;
        
        // Getters
        public String getProductId() { return productId; }
        public String getProductName() { return productName; }
        public String getDescription() { return description; }
        public BigDecimal getPrice() { return price; }
        public Integer getAvailableQuantity() { return availableQuantity; }
        public Integer getReservedQuantity() { return reservedQuantity; }
        
        // Setters
        public void setProductId(String productId) { this.productId = productId; }
        public void setProductName(String productName) { this.productName = productName; }
        public void setDescription(String description) { this.description = description; }
        public void setPrice(BigDecimal price) { this.price = price; }
        public void setAvailableQuantity(Integer availableQuantity) { this.availableQuantity = availableQuantity; }
        public void setReservedQuantity(Integer reservedQuantity) { this.reservedQuantity = reservedQuantity; }
    }
    
    /**
     * Request DTO for adding product (matches inventory service).
     */
    private static class ProductController {
        public static class AddProductRequest {
            private String productId;
            private String productName;
            private String description;
            private BigDecimal price;
            private Integer quantity;
            
            public AddProductRequest(String productId, String productName, String description, 
                                   BigDecimal price, Integer quantity) {
                this.productId = productId;
                this.productName = productName;
                this.description = description;
                this.price = price;
                this.quantity = quantity;
            }
            
            // Getters
            public String getProductId() { return productId; }
            public String getProductName() { return productName; }
            public String getDescription() { return description; }
            public BigDecimal getPrice() { return price; }
            public Integer getQuantity() { return quantity; }
        }
    }
}

