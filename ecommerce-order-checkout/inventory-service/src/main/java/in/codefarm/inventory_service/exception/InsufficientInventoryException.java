package in.codefarm.inventory_service.exception;

/**
 * Exception thrown when there is insufficient inventory for a product.
 * This is a business logic failure - non-retryable, send directly to DLT.
 */
public class InsufficientInventoryException extends RuntimeException {
    
    private final String productId;
    private final int requestedQuantity;
    private final int availableQuantity;
    
    public InsufficientInventoryException(String productId, int requestedQuantity, int availableQuantity) {
        super(String.format("Insufficient inventory: productId=%s, requested=%d, available=%d",
            productId, requestedQuantity, availableQuantity));
        this.productId = productId;
        this.requestedQuantity = requestedQuantity;
        this.availableQuantity = availableQuantity;
    }
    
    public String getProductId() {
        return productId;
    }
    
    public int getRequestedQuantity() {
        return requestedQuantity;
    }
    
    public int getAvailableQuantity() {
        return availableQuantity;
    }
}

