package in.codefarm.inventory_service.controller;

import in.codefarm.inventory_service.entity.Product;
import in.codefarm.inventory_service.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * REST controller for managing products in inventory.
 * Used by Order Service FE to add and list products.
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Slf4j
public class ProductController {
    
    private final ProductService productService;
    
    /**
     * Get all products.
     * Used by Order Service FE to display available products.
     */
    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts() {
        log.info("Getting all products");
        List<Product> products = productService.getAllProducts();
        return ResponseEntity.ok(products);
    }
    
    /**
     * Get a product by ID.
     */
    @GetMapping("/{productId}")
    public ResponseEntity<Product> getProduct(@PathVariable String productId) {
        log.info("Getting product: productId={}", productId);
        return productService.getProduct(productId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Add a new product to inventory.
     * Used by Order Service FE to add products before creating orders.
     */
    @PostMapping
    public ResponseEntity<Product> addProduct(@RequestBody AddProductRequest request) {
        log.info("Adding product: productId={}, productName={}, price={}, quantity={}", 
            request.getProductId(), request.getProductName(), request.getPrice(), request.getQuantity());
        
        Product product = productService.addProduct(
            request.getProductId(),
            request.getProductName(),
            request.getDescription(),
            request.getPrice(),
            request.getQuantity()
        );
        
        return ResponseEntity.status(HttpStatus.CREATED).body(product);
    }
    
    /**
     * Update product quantity.
     */
    @PutMapping("/{productId}/quantity")
    public ResponseEntity<Product> updateQuantity(
            @PathVariable String productId,
            @RequestBody UpdateQuantityRequest request) {
        log.info("Updating product quantity: productId={}, quantity={}", productId, request.getQuantity());
        
        Product product = productService.updateQuantity(productId, request.getQuantity());
        return ResponseEntity.ok(product);
    }
    
    /**
     * Request DTO for adding a product.
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class AddProductRequest {
        private String productId;
        private String productName;
        private String description;
        private BigDecimal price;
        private Integer quantity;
    }
    
    /**
     * Request DTO for updating quantity.
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class UpdateQuantityRequest {
        private Integer quantity;
    }
}

