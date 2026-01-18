package in.codefarm.inventory_service.repository;

import in.codefarm.inventory_service.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    
    Optional<Reservation> findByReservationId(String reservationId);
    
    List<Reservation> findByOrderId(String orderId);
    
    List<Reservation> findByStatus(Reservation.ReservationStatus status);
}

