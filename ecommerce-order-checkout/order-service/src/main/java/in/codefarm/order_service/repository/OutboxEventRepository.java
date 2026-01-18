package in.codefarm.order_service.repository;

import in.codefarm.order_service.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {
    // Debezium handles event publishing, so we don't need to query this table
    // It's only for writing events transactionally
}

