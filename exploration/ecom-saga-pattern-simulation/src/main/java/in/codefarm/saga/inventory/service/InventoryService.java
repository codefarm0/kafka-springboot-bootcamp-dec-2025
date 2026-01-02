package in.codefarm.saga.inventory.service;

import in.codefarm.saga.inventory.entity.InventoryReservationEntity;
import in.codefarm.saga.inventory.repository.InventoryReservationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class InventoryService {
    
    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);
    
    private final InventoryReservationRepository reservationRepository;
    
    public InventoryService(InventoryReservationRepository reservationRepository) {
        this.reservationRepository = reservationRepository;
    }
    
    @Transactional
    public InventoryReservationEntity reserveInventory(String reservationId, String orderId,
                                                       String productId, Integer quantity,
                                                       String status, String transactionId) {
        var reservation = new InventoryReservationEntity(
            reservationId,
            orderId,
            productId,
            quantity,
            status,
            java.time.LocalDateTime.now(),
            transactionId
        );
        
        return reservationRepository.save(reservation);
    }
    
    public Optional<InventoryReservationEntity> findByReservationId(String reservationId) {
        return reservationRepository.findByReservationId(reservationId);
    }
    
    public Optional<InventoryReservationEntity> findByOrderId(String orderId) {
        return reservationRepository.findByOrderId(orderId);
    }
}

