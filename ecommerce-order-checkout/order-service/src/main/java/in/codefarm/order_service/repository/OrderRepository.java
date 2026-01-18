package in.codefarm.order_service.repository;

import in.codefarm.order_service.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, String> {
    
    List<Order> findByCustomerId(String customerId);
    
    List<Order> findByStatus(Order.OrderStatus status);
    
    Optional<Order> findByIdAndCustomerId(String id, String customerId);
}

