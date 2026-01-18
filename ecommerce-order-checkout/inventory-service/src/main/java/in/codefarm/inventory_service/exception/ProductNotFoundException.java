package in.codefarm.inventory_service.exception;

/**
 * Exception thrown when a product is not found in inventory.
 * This is a business logic failure - non-retryable, send directly to DLT.
 */
public class ProductNotFoundException extends RuntimeException {
    
    private final String productId;
    
    public ProductNotFoundException(String productId) {
        super("Product not found: productId=" + productId);
        this.productId = productId;
    }
    
    public ProductNotFoundException(String productId, Throwable cause) {
        super("Product not found: productId=" + productId, cause);
        this.productId = productId;
    }
    
    public String getProductId() {
        return productId;
    }
}

