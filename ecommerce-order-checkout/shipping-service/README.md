# Shipping Service

Shipping Service creates shipping labels for orders in the e-commerce system. It consumes inventory events, creates shipping labels via shipping carrier API, and publishes shipping status events.

## Features

- ✅ **Shipping Label Creation**: Creates shipping labels via shipping carrier API
- ✅ **Idempotency**: Exactly-once processing using idempotency keys
- ✅ **CloudEvents**: Standardized event format (CNCF CloudEvents)
- ✅ **Event-Driven**: Consumes inventory and order events, publishes shipping events
- ✅ **Transactional Publishing**: Uses transactional Kafka producer for exactly-once semantics

## Architecture

### Event Flow

```
OrderPlacedEvent (orders topic) → Cache order data
InventoryReservedEvent (inventory topic) → Create Shipping Label → 
ShippingLabelCreatedEvent / ShippingFailedEvent (shipping topic)
```

### Shipping Label Creation

1. **Cache Order Data**: When `OrderPlacedEvent` is received, cache order data (for shipping address)
2. **Process Inventory Event**: When `InventoryReservedEvent` is received:
   - Check idempotency
   - Get cached order data
   - Create shipping label via shipping API
   - Update shipment status
   - Publish shipping event

### Idempotency

Shipping Service implements idempotency using:
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

### Shipments Table
- `id` (BIGINT): Primary key
- `shipment_id` (VARCHAR): Unique shipment ID
- `order_id` (VARCHAR): Order ID
- `customer_id` (VARCHAR): Customer ID
- `shipping_address` (TEXT): Shipping address
- `tracking_number` (VARCHAR): Carrier tracking number
- `carrier` (VARCHAR): Shipping carrier (FedEx, UPS, DHL, USPS)
- `status` (VARCHAR): Shipment status (PENDING, LABEL_CREATED, IN_TRANSIT, DELIVERED, FAILED)
- `failure_reason` (TEXT): Failure reason if shipping failed
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
- **`inventory`**: Inventory management events
  - CloudEvents type: `com.ecommerce.inventory.reserved` → Create shipping label

### Published Topics
- **`shipping`**: Shipping events
  - CloudEvents type: `com.ecommerce.shipping.label.created` (success)
  - CloudEvents type: `com.ecommerce.shipping.failed` (failure)

## Shipping Label Creation Flow

1. **Consume OrderPlacedEvent** from `orders` topic → Cache order data
2. **Consume InventoryReservedEvent** from `inventory` topic
3. **Idempotency Check**: Verify event hasn't been processed
4. **Get Cached Order Data**: Retrieve shipping address from cache
5. **Create Shipping Label** via shipping carrier API
6. **Update Shipment Status** (LABEL_CREATED or FAILED)
7. **Save Idempotency Key** to prevent reprocessing
8. **Publish Shipping Event** to `shipping` topic

## Configuration

### Application Properties

See `src/main/resources/application.yml` for configuration:

- **Database**: MySQL connection settings (port 3309)
- **Kafka**: Bootstrap servers, consumer group, producer settings
- **JPA**: Hibernate configuration

### Environment Variables

- `SPRING_DATASOURCE_URL`: Database URL (default: `jdbc:mysql://localhost:3309/shipping_db`)
- `SPRING_DATASOURCE_USERNAME`: Database username (default: `root`)
- `SPRING_DATASOURCE_PASSWORD`: Database password (default: `rootpassword`)
- `SPRING_KAFKA_BOOTSTRAP_SERVERS`: Kafka bootstrap servers (default: `localhost:9092`)

## Running the Service

### Prerequisites

1. **Infrastructure**: Kafka and Shipping DB must be running
   ```bash
   cd implementation
   docker-compose up -d
   ```

2. **Order Service**: Should be running and publishing order events
3. **Inventory Service**: Should be running and publishing inventory events

### Build and Run

```bash
# Build
./gradlew build

# Run
./gradlew bootRun
```

The service will start on port `8083`.

## Testing

### Unit Tests
```bash
./gradlew test
```

### Integration Tests
Integration tests use Testcontainers for Kafka and MySQL.

## Monitoring

- **Application Logs**: Check logs for shipping label creation
- **Kafka Topics**: Monitor `shipping` topic for shipping events
- **Database**: Check `shipments` and `idempotency_keys` tables
- **Tracking Numbers**: Monitor shipments table for tracking numbers

## Shipping API

The service includes a **mock shipping API client** for testing:
- 85% success rate
- 15% failure rate (simulated)
- Random tracking numbers
- Random carriers (FedEx, UPS, DHL, USPS)
- Simulated processing delay (150-400ms)

In production, replace `ShippingApiClient` with integration to a real shipping carrier API (FedEx, UPS, DHL, etc.).

## Order Data Caching

The service uses an in-memory cache (`ConcurrentHashMap`) to store order data:
- **Key**: Order ID
- **Value**: OrderPlacedEventData (shipping address)

**Note**: In production, consider using Redis or a database table for distributed caching.

## Troubleshooting

### Shipping Label Not Creating

1. Check if Inventory Service is publishing events
2. Verify order data is cached (check logs for "Order data cached")
3. Check consumer group is consuming from `inventory` topic
4. Review application logs for errors

### Shipping API Errors

1. Check shipping API client logs
2. Verify shipping API configuration
3. Review failure reasons in shipments table

### Duplicate Processing

1. Verify idempotency keys table is working
2. Check if event IDs are unique
3. Review idempotency check logic

