package in.codefarm.streams.dashboard;

import in.codefarm.streams.analytics.controller.OrderAnalyticsController;
import in.codefarm.streams.analytics.dto.CustomerOrderCount;
import in.codefarm.streams.analytics.dto.CustomerTotalAmount;
import in.codefarm.streams.controller.OrderController;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Controller
public class DashboardController {
    
    private final OrderAnalyticsController analyticsController;
    
    public DashboardController(OrderAnalyticsController analyticsController) {
        this.analyticsController = analyticsController;
    }
    
    @GetMapping("/")
    public String root() {
        return "redirect:/dashboard";
    }
    
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        // Get all analytics data
        ResponseEntity<Map<String, Long>> countsResponse = analyticsController.getAllOrderCounts();
        ResponseEntity<Map<String, BigDecimal>> totalsResponse = analyticsController.getAllTotalAmounts();
        
        Map<String, Long> counts = countsResponse.getStatusCode().is2xxSuccessful() 
            ? countsResponse.getBody() 
            : new HashMap<>();
        Map<String, BigDecimal> totals = totalsResponse.getStatusCode().is2xxSuccessful() 
            ? totalsResponse.getBody() 
            : new HashMap<>();
        
        model.addAttribute("orderCounts", counts);
        model.addAttribute("totalAmounts", totals);
        model.addAttribute("customerCount", counts.size());
        
        // Calculate total orders and total revenue
        long totalOrders = counts.values().stream().mapToLong(Long::longValue).sum();
        BigDecimal totalRevenue = totals.values().stream()
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        model.addAttribute("totalOrders", totalOrders);
        model.addAttribute("totalRevenue", totalRevenue);
        
        return "dashboard";
    }
    
    @GetMapping("/dashboard/order-form")
    public String orderForm() {
        return "order-form";
    }
}

