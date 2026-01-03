# E-commerce Saga Pattern Simulation

This project demonstrates the Saga Pattern using Kafka event choreography for distributed transaction coordination.

Get the deep dive details about SAGA pattern here - [Saga Pattern and Event Choreography](https://codefarm0.medium.com/saga-pattern-and-event-choreography-day-10-0b3d88fe8b02)

## Architecture

The saga flow:
1. **Order Service** → Creates order → Publishes `OrderPlaced` event
2. **Payment Service** → Processes payment → Publishes `PaymentProcessed` event
3. **Inventory Service** → Reserves inventory → Publishes `InventoryReserved` event
4. **Email Service** → Sends confirmation email

## Event Metadata

All events include metadata for tracing and versioning:
- `eventType`: Type of event (e.g., "OrderPlaced", "PaymentProcessed")
- `eventVersion`: Event schema version (e.g., "1.0")
- `source`: Service that published the event
- `transactionId`: Correlation ID for saga tracking
- `timestamp`: Event creation time

## Project Structure

```
src/main/java/in/codefarm/saga/
├── event/              # Event definitions with metadata
├── order/              # Order Service
│   ├── entity/
│   ├── repository/
│   ├── service/
│   ├── controller/
│   └── consumer/       # Compensation handler
├── payment/            # Payment Service
│   ├── entity/
│   ├── repository/
│   ├── service/
│   ├── consumer/       # OrderPlaced consumer
│   └── service/        # PaymentEventProducer
├── inventory/          # Inventory Service
│   ├── entity/
│   ├── repository/
│   ├── service/
│   └── consumer/       # PaymentProcessed consumer
├── email/              # Email Service
│   └── consumer/        # InventoryReserved consumer
└── config/             # Kafka configuration
```

## Setup

1. Start Kafka:
```bash
docker-compose up -d
```

2. Create topics:
```bash
docker exec -it kafka kafka-topics.sh --create \
  --bootstrap-server localhost:9092 \
  --replication-factor 1 \
  --partitions 3 \
  --topic orders

docker exec -it kafka kafka-topics.sh --create \
  --bootstrap-server localhost:9092 \
  --replication-factor 1 \
  --partitions 3 \
  --topic payments

docker exec -it kafka kafka-topics.sh --create \
  --bootstrap-server localhost:9092 \
  --replication-factor 1 \
  --partitions 3 \
  --topic inventory
```

3. Run the application:
```bash
./gradlew bootRun
```

## Testing

### Success Case (Amount < 1000)
```bash
curl -X POST http://localhost:8088/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "customer-123",
    "productId": "product-456",
    "quantity": 2,
    "totalAmount": 99.99
  }'
```

Expected flow:
- Order created → OrderPlaced event
- Payment processed (SUCCESS) → PaymentProcessed event
- Inventory reserved (RESERVED) → InventoryReserved event
- Email sent

### Failure Case (Amount > 1000)
```bash
curl -X POST http://localhost:8088/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "customer-456",
    "productId": "product-789",
    "quantity": 1,
    "totalAmount": 1500.00
  }'
```

Expected flow:
- Order created → OrderPlaced event
- Payment processed (FAILED) → PaymentProcessed event
- Saga stops (inventory and email not processed)

## Features

- ✅ Event metadata (eventType, eventVersion, source, transactionId, timestamp)
- ✅ Idempotent processing (duplicate detection)
- ✅ Compensation handling (refund on inventory failure)
- ✅ Clean separation of concerns
- ✅ Proper DTOs (no Map usage)
- ✅ Minimal logging
- ✅ Best practices from previous days

## API Endpoints

- `POST /api/orders` - Place an order (triggers saga)
- `GET /api/orders` - Get all orders
- `GET /api/orders/{orderId}` - Get order by ID

## Event Flow

```
Order Service
  ↓ OrderPlaced (eventType, eventVersion, source, transactionId, timestamp)
Payment Service
  ↓ PaymentProcessed (SUCCESS/FAILED)
Inventory Service
  ↓ InventoryReserved (RESERVED/UNAVAILABLE)
Email Service
  ↓ Email Sent
```

## Compensation Flow

If inventory is unavailable:
1. Inventory Service publishes `InventoryReserved` (UNAVAILABLE)
2. Payment Service consumes it → Refunds payment → Publishes `PaymentRefunded`
3. Order Service consumes `PaymentRefunded` → Cancels order → Publishes `OrderCancelled`

