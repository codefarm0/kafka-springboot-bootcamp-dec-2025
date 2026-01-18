# Payment Service

Payment Service processes payments for orders in the e-commerce system. It consumes order events, processes payments through a payment gateway, and publishes payment status events.

## Features

- ✅ **Payment Processing**: Processes payments for orders via payment gateway
- ✅ **Idempotency**: Exactly-once processing using idempotency keys
- ✅ **CloudEvents**: Standardized event format (CNCF CloudEvents)
- ✅ **Event-Driven**: Consumes order events, publishes payment events
- ✅ **Transactional Publishing**: Uses transactional Kafka producer for exactly-once semantics

## Architecture

### Event Flow

```
OrderPlacedEvent (orders topic) → Payment Service → Process Payment → 
PaymentProcessedEvent / PaymentFailedEvent (payments topic)
```

### Idempotency

Payment Service implements idempotency using:
- **Idempotency Key Table**: Stores processed event IDs
- **CloudEvents ID**: Used as idempotency key
- **Duplicate Detection**: Prevents reprocessing of the same event

## Technology Stack

- **Spring Boot 4.0.1**
- **Spring Data JPA** (MySQL)
- **Spring Kafka** (CloudEvents)
- **CloudEvents Java SDK 2.5.0**
- **MySQL 8.0**
- **Mock Payment Gateway** (for testing)

## Database Schema

### Payments Table
- `id` (BIGINT): Primary key
- `payment_id` (VARCHAR): Unique payment ID
- `order_id` (VARCHAR): Order ID
- `customer_id` (VARCHAR): Customer ID
- `amount` (DECIMAL): Payment amount
- `status` (VARCHAR): Payment status (PENDING, PROCESSING, COMPLETED, FAILED, REFUNDED)
- `payment_method` (VARCHAR): Payment method
- `transaction_id` (VARCHAR): Gateway transaction ID
- `failure_reason` (TEXT): Failure reason if payment failed
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
  - CloudEvents type: `com.ecommerce.order.placed` → Process payment

### Published Topics
- **`payments`**: Payment processing events
  - CloudEvents type: `com.ecommerce.payment.processed` (success)
  - CloudEvents type: `com.ecommerce.payment.failed` (failure)

## Payment Processing Flow

1. **Consume OrderPlacedEvent** from `orders` topic
2. **Idempotency Check**: Verify event hasn't been processed
3. **Create Payment Record** with PENDING status
4. **Process Payment** via payment gateway
5. **Update Payment Status** (COMPLETED or FAILED)
6. **Save Idempotency Key** to prevent reprocessing
7. **Publish Payment Event** to `payments` topic

## Configuration

### Application Properties

See `src/main/resources/application.yml` for configuration:

- **Database**: MySQL connection settings (port 3307)
- **Kafka**: Bootstrap servers, consumer group, producer settings
- **JPA**: Hibernate configuration

### Environment Variables

- `SPRING_DATASOURCE_URL`: Database URL (default: `jdbc:mysql://localhost:3307/payment_db`)
- `SPRING_DATASOURCE_USERNAME`: Database username (default: `root`)
- `SPRING_DATASOURCE_PASSWORD`: Database password (default: `rootpassword`)
- `SPRING_KAFKA_BOOTSTRAP_SERVERS`: Kafka bootstrap servers (default: `localhost:9092`)

## Running the Service

### Prerequisites

1. **Infrastructure**: Kafka and Payment DB must be running
   ```bash
   cd implementation
   docker-compose up -d
   ```

2. **Order Service**: Should be running and publishing order events

### Build and Run

```bash
# Build
./gradlew build

# Run
./gradlew bootRun
```

The service will start on port `8081`.

## Testing

### Unit Tests
```bash
./gradlew test
```

### Integration Tests
Integration tests use Testcontainers for Kafka and MySQL.

## Monitoring

- **Application Logs**: Check logs for payment processing
- **Kafka Topics**: Monitor `payments` topic for payment events
- **Database**: Check `payments` and `idempotency_keys` tables
- **Idempotency**: Verify duplicate events are not reprocessed

## Payment Gateway

The service includes a **mock payment gateway** for testing:
- 90% success rate
- 10% failure rate (simulated)
- Random transaction IDs
- Simulated processing delay (100-300ms)

In production, replace `PaymentGatewayClient` with integration to a real payment gateway (Stripe, PayPal, etc.).

## Troubleshooting

### Payment Not Processing

1. Check if Order Service is publishing events
2. Verify consumer group is consuming from `orders` topic
3. Check application logs for errors

### Duplicate Processing

1. Verify idempotency keys table is working
2. Check if event IDs are unique
3. Review idempotency check logic

### Payment Gateway Errors

1. Check payment gateway client logs
2. Verify payment gateway configuration
3. Review failure reasons in payments table

