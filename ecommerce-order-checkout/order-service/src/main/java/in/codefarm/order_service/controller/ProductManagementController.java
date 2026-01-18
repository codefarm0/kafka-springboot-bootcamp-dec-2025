package in.codefarm.order_service.controller;

import in.codefarm.order_service.service.InventoryServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

/**
 * Controller for managing products in inventory service.
 * Provides UI for adding and viewing products.
 */
@Controller
@RequestMapping("/products")
@RequiredArgsConstructor
@Slf4j
public class ProductManagementController {
    
    private final InventoryServiceClient inventoryServiceClient;
    
    /**
     * Product management page - list all products.
     */
    @GetMapping
    public String listProducts(Model model) {
        try {
            List<InventoryServiceClient.Product> products = inventoryServiceClient.getAllProducts();
            model.addAttribute("products", products);
            model.addAttribute("productCount", products.size());
        } catch (Exception e) {
            log.error("Failed to fetch products", e);
            model.addAttribute("error", "Failed to fetch products: " + e.getMessage());
            model.addAttribute("products", List.of());
            model.addAttribute("productCount", 0);
        }
        return "products";
    }
    
    /**
     * Show add product form.
     */
    @GetMapping("/add")
    public String showAddProductForm(Model model) {
        model.addAttribute("productRequest", new AddProductRequest());
        return "add-product";
    }
    
    /**
     * Add a new product.
     */
    @PostMapping("/add")
    public String addProduct(
            @ModelAttribute("productRequest") AddProductRequest request,
            RedirectAttributes redirectAttributes) {
        
        try {
            if (request.getProductId() == null || request.getProductId().trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Product ID is required");
                return "redirect:/products/add";
            }
            
            if (request.getProductName() == null || request.getProductName().trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Product Name is required");
                return "redirect:/products/add";
            }
            
            if (request.getPrice() == null || request.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
                redirectAttributes.addFlashAttribute("error", "Price must be greater than 0");
                return "redirect:/products/add";
            }
            
            if (request.getQuantity() == null || request.getQuantity() <= 0) {
                redirectAttributes.addFlashAttribute("error", "Quantity must be greater than 0");
                return "redirect:/products/add";
            }
            
            inventoryServiceClient.addProduct(
                request.getProductId().trim(),
                request.getProductName().trim(),
                request.getDescription() != null ? request.getDescription().trim() : "",
                request.getPrice(),
                request.getQuantity()
            );
            
            redirectAttributes.addFlashAttribute("success", 
                "Product added successfully: " + request.getProductName());
            return "redirect:/products";
            
        } catch (Exception e) {
            log.error("Failed to add product", e);
            redirectAttributes.addFlashAttribute("error", 
                "Failed to add product: " + e.getMessage());
            return "redirect:/products/add";
        }
    }
    
    /**
     * Request DTO for adding a product.
     */
    public static class AddProductRequest {
        private String productId;
        private String productName;
        private String description;
        private BigDecimal price;
        private Integer quantity;
        
        // Getters and setters
        public String getProductId() { return productId; }
        public void setProductId(String productId) { this.productId = productId; }
        
        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public BigDecimal getPrice() { return price; }
        public void setPrice(BigDecimal price) { this.price = price; }
        
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
    }
}

