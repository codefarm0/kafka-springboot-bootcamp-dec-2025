package in.codefarm.order_service.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {
    
    @NotNull(message = "Customer ID is required")
    @NotEmpty(message = "Customer ID cannot be empty")
    private String customerId;
    
    @Valid
    @NotEmpty(message = "Order items are required")
    private List<OrderItemRequest> items;
    
    private String shippingAddress;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemRequest {
        @NotNull(message = "Product ID is required")
        @NotEmpty(message = "Product ID cannot be empty")
        private String productId;
        
        @NotNull(message = "Product name is required")
        @NotEmpty(message = "Product name cannot be empty")
        private String productName;
        
        @NotNull(message = "Quantity is required")
        private Integer quantity;
        
        @NotNull(message = "Unit price is required")
        private java.math.BigDecimal unitPrice;
    }
}

