package in.codefarm.order_service.controller;

import in.codefarm.order_service.dto.CreateOrderRequest;
import in.codefarm.order_service.dto.OrderResponse;
import in.codefarm.order_service.service.InventoryServiceClient;
import in.codefarm.order_service.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Web controller for Thymeleaf frontend.
 * Provides UI for placing orders and viewing order status.
 */
@Controller
@RequestMapping("/")
@RequiredArgsConstructor
@Slf4j
public class WebController {
    
    private final OrderService orderService;
    private final InventoryServiceClient inventoryServiceClient;
    
    /**
     * Home page - Order form.
     */
    @GetMapping
    public String home(Model model) {
        model.addAttribute("orderRequest", new CreateOrderRequest());
        model.addAttribute("sampleProducts", getSampleProducts());
        return "index";
    }
    
    /**
     * Batch order page.
     */
    @GetMapping("/batch")
    public String batchOrder(Model model) {
        model.addAttribute("orderRequest", new CreateOrderRequest());
        model.addAttribute("sampleProducts", getSampleProducts());
        return "batch";
    }
    
    /**
     * Dashboard - View all orders.
     */
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        List<OrderResponse> orders = orderService.getAllOrders();
        model.addAttribute("orders", orders);
        model.addAttribute("totalOrders", orders.size());
        
        // Count orders by status
        long pending = orders.stream().filter(o -> "PENDING".equals(o.getStatus())).count();
        long confirmed = orders.stream().filter(o -> "CONFIRMED".equals(o.getStatus())).count();
        long cancelled = orders.stream().filter(o -> "CANCELLED".equals(o.getStatus())).count();
        
        model.addAttribute("pendingCount", pending);
        model.addAttribute("confirmedCount", confirmed);
        model.addAttribute("cancelledCount", cancelled);
        
        return "dashboard";
    }
    
    /**
     * View single order.
     */
    @GetMapping("/orders/{orderId}")
    public String viewOrder(@PathVariable String orderId, Model model) {
        try {
            OrderResponse order = orderService.getOrder(orderId);
            model.addAttribute("order", order);
            return "order-detail";
        } catch (Exception e) {
            model.addAttribute("error", "Order not found: " + orderId);
            return "error";
        }
    }
    
    /**
     * Place a single order.
     */
    @PostMapping("/orders")
    public String createOrder(
            @Valid @ModelAttribute("orderRequest") CreateOrderRequest request,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        
        if (bindingResult.hasErrors()) {
            model.addAttribute("sampleProducts", getSampleProducts());
            return "index";
        }
        
        try {
            OrderResponse order = orderService.createOrder(request);
            redirectAttributes.addFlashAttribute("success", 
                "Order created successfully! Order ID: " + order.getId());
            return "redirect:/dashboard";
        } catch (Exception e) {
            log.error("Failed to create order", e);
            model.addAttribute("error", "Failed to create order: " + e.getMessage());
            model.addAttribute("sampleProducts", getSampleProducts());
            return "index";
        }
    }
    
    /**
     * Place batch orders.
     */
    @PostMapping("/orders/batch")
    public String createBatchOrders(
            @Valid @ModelAttribute("orderRequest") CreateOrderRequest request,
            @RequestParam(defaultValue = "10") int count,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        
        if (bindingResult.hasErrors()) {
            model.addAttribute("sampleProducts", getSampleProducts());
            return "batch";
        }
        
        if (count < 1 || count > 100) {
            model.addAttribute("error", "Batch count must be between 1 and 100");
            model.addAttribute("sampleProducts", getSampleProducts());
            return "batch";
        }
        
        try {
            List<OrderResponse> orders = orderService.createBatchOrders(request, count);
            redirectAttributes.addFlashAttribute("success", 
                String.format("Batch orders created successfully! Created %d out of %d orders.", 
                    orders.size(), count));
            return "redirect:/dashboard";
        } catch (Exception e) {
            log.error("Failed to create batch orders", e);
            model.addAttribute("error", "Failed to create batch orders: " + e.getMessage());
            model.addAttribute("sampleProducts", getSampleProducts());
            return "batch";
        }
    }
    
    /**
     * Get products from inventory service for the form.
     * Falls back to sample products if inventory service is unavailable.
     */
    private List<SampleProduct> getSampleProducts() {
        try {
            List<InventoryServiceClient.Product> products = inventoryServiceClient.getAllProducts();
            
            if (products.isEmpty()) {
                log.warn("No products found in inventory service, using sample products");
                return getFallbackProducts();
            }
            
            return products.stream()
                .map(p -> new SampleProduct(p.getProductId(), p.getProductName(), p.getPrice()))
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            log.warn("Failed to fetch products from inventory service: {}. Using fallback products.", e.getMessage());
            return getFallbackProducts();
        }
    }
    
    /**
     * Fallback products if inventory service is unavailable.
     */
    private List<SampleProduct> getFallbackProducts() {
        return List.of(
            new SampleProduct("product-1", "Sample Product 1", 29.99),
            new SampleProduct("product-2", "Sample Product 2", 49.99),
            new SampleProduct("product-3", "Sample Product 3", 19.99)
        );
    }
    
    /**
     * Sample product data class.
     */
    public static class SampleProduct {
        private String productId;
        private String productName;
        private double price;
        
        public SampleProduct(String productId, String productName, double price) {
            this.productId = productId;
            this.productName = productName;
            this.price = price;
        }
        
        public String getProductId() { return productId; }
        public String getProductName() { return productName; }
        public double getPrice() { return price; }
    }
}

