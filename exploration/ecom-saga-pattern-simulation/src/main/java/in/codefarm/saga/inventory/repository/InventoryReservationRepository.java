package in.codefarm.saga.inventory.repository;

import in.codefarm.saga.inventory.entity.InventoryReservationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InventoryReservationRepository extends JpaRepository<InventoryReservationEntity, Long> {
    Optional<InventoryReservationEntity> findByReservationId(String reservationId);
    Optional<InventoryReservationEntity> findByOrderId(String orderId);
}

