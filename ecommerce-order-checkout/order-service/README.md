# Order Service

Order Service is the core microservice in the e-commerce system. It handles order creation and orchestrates the order processing flow using the Saga pattern (event choreography).

## Features

- ✅ **Order Management**: Create and retrieve orders
- ✅ **Outbox Pattern**: Transactional event publishing via Debezium CDC
- ✅ **CloudEvents**: Standardized event format (CNCF CloudEvents)
- ✅ **Saga Orchestration**: Event-driven order processing flow
- ✅ **Idempotency**: Exactly-once semantics with transactional producers

## Architecture

### Outbox Pattern with Debezium CDC

Instead of a scheduled poller, Order Service uses **Debezium Change Data Capture (CDC)** for real-time event publishing:

1. Order is created and saved to `orders` table
2. Event is written to `outbox_events` table in the **same transaction**
3. Debezium captures the change from MySQL binlog
4. Event is automatically published to Kafka in CloudEvents format

### Event Flow

```
Order Created → Outbox Table → Debezium CDC → Kafka → Other Services
```

## Technology Stack

- **Spring Boot 4.0.1**
- **Spring Data JPA** (MySQL)
- **Spring Kafka** (CloudEvents)
- **CloudEvents Java SDK 2.5.0**
- **MySQL 8.0** (with binlog enabled)
- **Debezium CDC** (via Kafka Connect)

## Database Schema

### Orders Table
- `id` (VARCHAR): Order ID
- `customer_id` (VARCHAR): Customer ID
- `total_amount` (DECIMAL): Total order amount
- `status` (VARCHAR): Order status (PENDING, CONFIRMED, COMPLETED, etc.)
- `shipping_address` (TEXT): Shipping address
- `created_at`, `updated_at` (TIMESTAMP): Timestamps

### Order Items Table
- `id` (BIGINT): Item ID
- `order_id` (VARCHAR): Foreign key to orders
- `product_id` (VARCHAR): Product ID
- `product_name` (VARCHAR): Product name
- `quantity` (INT): Quantity
- `unit_price` (DECIMAL): Unit price
- `total_price` (DECIMAL): Total price

### Outbox Events Table
- `id` (BIGINT): Event ID
- `aggregate_id` (VARCHAR): Order ID (used as Kafka key)
- `event_type` (VARCHAR): CloudEvents type (e.g., `com.ecommerce.order.placed`)
- `source` (VARCHAR): CloudEvents source (`/order-service`)
- `payload` (TEXT): Event data (JSON)
- `correlation_id` (VARCHAR): For distributed tracing
- `created_at` (TIMESTAMP): Event timestamp

## API Endpoints

### Create Order
```http
POST /api/orders
Content-Type: application/json

{
  "customerId": "customer-123",
  "items": [
    {
      "productId": "product-1",
      "productName": "Product 1",
      "quantity": 2,
      "unitPrice": 29.99
    }
  ],
  "shippingAddress": "123 Main St, City, Country"
}
```

**Response:**
```json
{
  "id": "order-550e8400-...",
  "customerId": "customer-123",
  "totalAmount": 59.98,
  "status": "PENDING",
  "items": [...],
  "createdAt": "2024-01-15T10:30:00",
  "updatedAt": "2024-01-15T10:30:00"
}
```

### Get Order
```http
GET /api/orders/{orderId}
```

## Kafka Topics

### Published Topics
- **`orders`**: All order lifecycle events are published to this topic
  - CloudEvents type: `com.ecommerce.order.placed` (when order is created)
  - Subject: Order ID
  - Source: `/order-service`
  - Data: Order details (orderId, customerId, totalAmount, items, shippingAddress)

### Consumed Topics
Order Service listens to the following topics and filters by CloudEvents type:

- **`payments`**: Payment processing events
  - CloudEvents type: `com.ecommerce.payment.processed` → Update order status to `PAYMENT_COMPLETED`
  - CloudEvents type: `com.ecommerce.payment.failed` → Update order status to `PAYMENT_FAILED`

- **`inventory`**: Inventory management events
  - CloudEvents type: `com.ecommerce.inventory.reserved` → Update order status to `INVENTORY_RESERVED`
  - CloudEvents type: `com.ecommerce.inventory.reservation.failed` → Update order status to `INVENTORY_RESERVATION_FAILED`

- **`shipping`**: Shipping events
  - CloudEvents type: `com.ecommerce.shipping.arranged` or `com.ecommerce.shipping.label.created` → Update order status to `SHIPPING_ARRANGED` then `COMPLETED`
  - CloudEvents type: `com.ecommerce.shipping.failed` → Update order status to `SHIPPING_FAILED`

## Order Status Flow

```
PENDING → PAYMENT_PROCESSING → PAYMENT_COMPLETED → 
INVENTORY_RESERVED → SHIPPING_ARRANGED → COMPLETED
```

If any step fails, status is updated accordingly (e.g., `PAYMENT_FAILED`, `INVENTORY_RESERVATION_FAILED`).

## Configuration

### Application Properties

See `src/main/resources/application.yml` for configuration:

- **Database**: MySQL connection settings
- **Kafka**: Bootstrap servers, consumer group, producer settings
- **JPA**: Hibernate configuration

### Environment Variables

- `SPRING_DATASOURCE_URL`: Database URL (default: `jdbc:mysql://localhost:3306/order_db`)
- `SPRING_DATASOURCE_USERNAME`: Database username (default: `root`)
- `SPRING_DATASOURCE_PASSWORD`: Database password (default: `rootpassword`)
- `SPRING_KAFKA_BOOTSTRAP_SERVERS`: Kafka bootstrap servers (default: `localhost:9092`)

## Running the Service

### Prerequisites

1. **Infrastructure**: Kafka, MySQL, Kafka Connect, Debezium must be running
   ```bash
   cd implementation
   docker-compose up -d
   ```

2. **Debezium Connector**: Deploy the connector
   ```bash
   ./scripts/deploy-debezium-connector.sh
   ```

### Build and Run

```bash
# Build
./gradlew build

# Run
./gradlew bootRun
```

The service will start on port `8080`.

## Testing

### Unit Tests
```bash
./gradlew test
```

### Integration Tests
Integration tests use Testcontainers for Kafka and MySQL.

## Monitoring

- **Application Logs**: Check logs for order creation and event processing
- **Kafka Topics**: Monitor `orders` topic (contains `com.ecommerce.order.placed` events)
- **Database**: Check `outbox_events` table for events waiting to be published
- **Debezium**: Monitor connector status via Kafka Connect REST API

## Troubleshooting

### Events Not Appearing in Kafka

1. Check Debezium connector status:
   ```bash
   curl http://localhost:8083/connectors/order-service-connector/status
   ```

2. Verify MySQL binlog is enabled:
   ```sql
   SHOW VARIABLES LIKE 'log_bin';
   ```

3. Check outbox_events table:
   ```sql
   SELECT * FROM outbox_events ORDER BY created_at DESC LIMIT 10;
   ```

### Order Creation Fails

1. Check database connection
2. Verify database schema is created
3. Check application logs for errors

## Next Steps

After Order Service is running:

1. **Payment Service**: Process payments and publish events
2. **Inventory Service**: Reserve inventory and publish events
3. **Shipping Service**: Arrange shipping and publish events
4. **Notification Service**: Send notifications

See the [Implementation Guide](../implementation/README.md) for the complete implementation plan.

