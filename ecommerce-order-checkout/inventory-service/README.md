# Inventory Service

Inventory Service manages product inventory and reserves items for orders in the e-commerce system. It consumes payment events, reserves inventory, and publishes reservation status events.

## Features

- ✅ **Inventory Management**: Manages product stock levels
- ✅ **Reservation System**: Reserves inventory for orders with pessimistic locking
- ✅ **Idempotency**: Exactly-once processing using idempotency keys
- ✅ **CloudEvents**: Standardized event format (CNCF CloudEvents)
- ✅ **Event-Driven**: Consumes payment and order events, publishes inventory events
- ✅ **Transactional Publishing**: Uses transactional Kafka producer for exactly-once semantics

## Architecture

### Event Flow

```
OrderPlacedEvent (orders topic) → Cache order data
PaymentProcessedEvent (payments topic) → Reserve Inventory → 
InventoryReservedEvent / InventoryReservationFailedEvent (inventory topic)
```

### Reservation Logic

1. **Cache Order Data**: When `OrderPlacedEvent` is received, cache order items
2. **Process Payment Event**: When `PaymentProcessedEvent` is received:
   - Check idempotency
   - Get cached order items
   - For each item: Check availability, reserve with pessimistic lock
   - Update product reserved quantity
   - Publish reservation event

### Idempotency

Inventory Service implements idempotency using:
- **Idempotency Key Table**: Stores processed event IDs
- **CloudEvents ID**: Used as idempotency key
- **Duplicate Detection**: Prevents reprocessing of the same event

## Technology Stack

- **Spring Boot 4.0.1**
- **Spring Data JPA** (MySQL)
- **Spring Kafka** (CloudEvents)
- **CloudEvents Java SDK 2.5.0**
- **MySQL 8.0**

## Database Schema

### Products Table
- `id` (BIGINT): Primary key
- `product_id` (VARCHAR): Unique product ID
- `product_name` (VARCHAR): Product name
- `description` (TEXT): Product description
- `price` (DECIMAL): Product price
- `available_quantity` (INT): Total available quantity
- `reserved_quantity` (INT): Currently reserved quantity
- `created_at`, `updated_at` (TIMESTAMP): Timestamps

### Reservations Table
- `id` (BIGINT): Primary key
- `reservation_id` (VARCHAR): Unique reservation ID
- `order_id` (VARCHAR): Order ID
- `product_id` (BIGINT): Foreign key to products
- `quantity` (INT): Reserved quantity
- `status` (VARCHAR): Reservation status (PENDING, RESERVED, FAILED, RELEASED)
- `failure_reason` (TEXT): Failure reason if reservation failed
- `created_at`, `updated_at` (TIMESTAMP): Timestamps

### Idempotency Keys Table
- `id` (BIGINT): Primary key
- `event_id` (VARCHAR): CloudEvents ID (unique, for idempotency)
- `order_id` (VARCHAR): Order ID
- `event_type` (VARCHAR): Event type
- `processed_at` (TIMESTAMP): Processing timestamp

## Kafka Topics

### Consumed Topics
- **`orders`**: Order lifecycle events
  - CloudEvents type: `com.ecommerce.order.placed` → Cache order data
- **`payments`**: Payment processing events
  - CloudEvents type: `com.ecommerce.payment.processed` → Reserve inventory

### Published Topics
- **`inventory`**: Inventory management events
  - CloudEvents type: `com.ecommerce.inventory.reserved` (success)
  - CloudEvents type: `com.ecommerce.inventory.reservation.failed` (failure)

## Inventory Reservation Flow

1. **Consume OrderPlacedEvent** from `orders` topic → Cache order items
2. **Consume PaymentProcessedEvent** from `payments` topic
3. **Idempotency Check**: Verify event hasn't been processed
4. **Get Cached Order Data**: Retrieve order items from cache
5. **For Each Order Item**:
   - Get product with pessimistic lock
   - Check if sufficient quantity available
   - Create reservation record
   - Update product reserved quantity
6. **Save Idempotency Key** to prevent reprocessing
7. **Publish Inventory Event** to `inventory` topic

## Configuration

### Application Properties

See `src/main/resources/application.yml` for configuration:

- **Database**: MySQL connection settings (port 3308)
- **Kafka**: Bootstrap servers, consumer group, producer settings
- **JPA**: Hibernate configuration

### Environment Variables

- `SPRING_DATASOURCE_URL`: Database URL (default: `jdbc:mysql://localhost:3308/inventory_db`)
- `SPRING_DATASOURCE_USERNAME`: Database username (default: `root`)
- `SPRING_DATASOURCE_PASSWORD`: Database password (default: `rootpassword`)
- `SPRING_KAFKA_BOOTSTRAP_SERVERS`: Kafka bootstrap servers (default: `localhost:9092`)

## Running the Service

### Prerequisites

1. **Infrastructure**: Kafka and Inventory DB must be running
   ```bash
   cd implementation
   docker-compose up -d
   ```

2. **Order Service**: Should be running and publishing order events
3. **Payment Service**: Should be running and publishing payment events

### Build and Run

```bash
# Build
./gradlew build

# Run
./gradlew bootRun
```

The service will start on port `8082`.

## Testing

### Unit Tests
```bash
./gradlew test
```

### Integration Tests
Integration tests use Testcontainers for Kafka and MySQL.

## Monitoring

- **Application Logs**: Check logs for inventory reservation processing
- **Kafka Topics**: Monitor `inventory` topic for reservation events
- **Database**: Check `reservations` and `idempotency_keys` tables
- **Product Stock**: Monitor `products` table for available/reserved quantities

## Order Data Caching

The service uses an in-memory cache (`ConcurrentHashMap`) to store order data:
- **Key**: Order ID
- **Value**: OrderPlacedEventData (order items)

**Note**: In production, consider using Redis or a database table for distributed caching.

## Troubleshooting

### Reservation Not Processing

1. Check if Payment Service is publishing events
2. Verify order data is cached (check logs for "Order data cached")
3. Check consumer group is consuming from `payments` topic
4. Review application logs for errors

### Insufficient Inventory

1. Check `products` table for available quantities
2. Verify `reserved_quantity` vs `available_quantity`
3. Review reservation failure events

### Duplicate Processing

1. Verify idempotency keys table is working
2. Check if event IDs are unique
3. Review idempotency check logic

