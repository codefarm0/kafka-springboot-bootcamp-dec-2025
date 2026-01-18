package in.codefarm.notification_service.repository;

import in.codefarm.notification_service.entity.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, Long> {
    
    Optional<IdempotencyKey> findByEventId(String eventId);
    
    boolean existsByEventId(String eventId);
}

