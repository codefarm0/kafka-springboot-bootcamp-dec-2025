package in.codefarm.inventory_service.service;

import in.codefarm.inventory_service.entity.Product;
import in.codefarm.inventory_service.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing products in inventory.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {
    
    private final ProductRepository productRepository;
    
    /**
     * Get all products.
     */
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }
    
    /**
     * Get a product by product ID.
     */
    public Optional<Product> getProduct(String productId) {
        return productRepository.findByProductId(productId);
    }
    
    /**
     * Add a new product to inventory.
     * If product already exists, updates it.
     */
    @Transactional
    public Product addProduct(String productId, String productName, String description, 
                             BigDecimal price, Integer quantity) {
        log.info("Adding/updating product: productId={}, productName={}, price={}, quantity={}", 
            productId, productName, price, quantity);
        
        Optional<Product> existingProduct = productRepository.findByProductId(productId);
        
        if (existingProduct.isPresent()) {
            // Update existing product
            Product product = existingProduct.get();
            product.setProductName(productName);
            product.setDescription(description);
            product.setPrice(price);
            product.setAvailableQuantity(quantity);
            // Don't reset reservedQuantity - keep existing reservations
            product = productRepository.save(product);
            log.info("Updated existing product: productId={}", productId);
            return product;
        } else {
            // Create new product
            Product product = Product.builder()
                .productId(productId)
                .productName(productName)
                .description(description)
                .price(price)
                .availableQuantity(quantity)
                .reservedQuantity(0)
                .build();
            
            product = productRepository.save(product);
            log.info("Created new product: productId={}, id={}", productId, product.getId());
            return product;
        }
    }
    
    /**
     * Update product quantity.
     */
    @Transactional
    public Product updateQuantity(String productId, Integer quantity) {
        log.info("Updating product quantity: productId={}, quantity={}", productId, quantity);
        
        Product product = productRepository.findByProductId(productId)
            .orElseThrow(() -> new RuntimeException("Product not found: " + productId));
        
        product.setAvailableQuantity(quantity);
        product = productRepository.save(product);
        
        log.info("Updated product quantity: productId={}, newQuantity={}", productId, quantity);
        return product;
    }
}

