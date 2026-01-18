# Notification Service

Notification Service sends email and SMS notifications for orders in the e-commerce system. It consumes shipping events, sends notifications via email/SMS services, and publishes notification status events.

## Features

- ✅ **Email Notifications**: Sends email notifications via email service
- ✅ **SMS Notifications**: Sends SMS notifications via SMS service
- ✅ **Idempotency**: Exactly-once processing using idempotency keys
- ✅ **CloudEvents**: Standardized event format (CNCF CloudEvents)
- ✅ **Event-Driven**: Consumes shipping events, publishes notification events
- ✅ **Transactional Publishing**: Uses transactional Kafka producer for exactly-once semantics

## Architecture

### Event Flow

```
ShippingLabelCreatedEvent (shipping topic) → Send Email & SMS → 
NotificationSentEvent / NotificationFailedEvent (notifications topic)
```

### Notification Sending

1. **Consume ShippingLabelCreatedEvent** from `shipping` topic
2. **Idempotency Check**: Verify event hasn't been processed
3. **Create Notification Records** for email and SMS
4. **Send Email** via email service
5. **Send SMS** via SMS service
6. **Update Notification Status** (SENT or FAILED)
7. **Save Idempotency Key** to prevent reprocessing
8. **Publish Notification Events** to `notifications` topic

### Idempotency

Notification Service implements idempotency using:
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

### Notifications Table
- `id` (BIGINT): Primary key
- `notification_id` (VARCHAR): Unique notification ID
- `order_id` (VARCHAR): Order ID
- `customer_id` (VARCHAR): Customer ID
- `notification_type` (VARCHAR): Type (ORDER_CONFIRMATION, ORDER_SHIPPED, etc.)
- `channel` (VARCHAR): Channel (EMAIL or SMS)
- `recipient` (VARCHAR): Email address or phone number
- `subject` (VARCHAR): Email subject (for email notifications)
- `message` (TEXT): Notification message
- `status` (VARCHAR): Notification status (PENDING, SENT, FAILED)
- `failure_reason` (TEXT): Failure reason if notification failed
- `created_at`, `updated_at` (TIMESTAMP): Timestamps

### Idempotency Keys Table
- `id` (BIGINT): Primary key
- `event_id` (VARCHAR): CloudEvents ID (unique, for idempotency)
- `order_id` (VARCHAR): Order ID
- `event_type` (VARCHAR): Event type
- `processed_at` (TIMESTAMP): Processing timestamp

## Kafka Topics

### Consumed Topics
- **`shipping`**: Shipping events
  - CloudEvents type: `com.ecommerce.shipping.label.created` → Send order confirmation

### Published Topics
- **`notifications`**: Notification events
  - CloudEvents type: `com.ecommerce.notification.sent` (success)
  - CloudEvents type: `com.ecommerce.notification.failed` (failure)

## Notification Sending Flow

1. **Consume ShippingLabelCreatedEvent** from `shipping` topic
2. **Idempotency Check**: Verify event hasn't been processed
3. **Create Email Notification** record
4. **Send Email** via email service
5. **Create SMS Notification** record
6. **Send SMS** via SMS service
7. **Update Notification Status** based on send results
8. **Save Idempotency Key** to prevent reprocessing
9. **Publish Notification Events** to `notifications` topic

## Configuration

### Application Properties

See `src/main/resources/application.yml` for configuration:

- **Database**: MySQL connection settings (port 3310)
- **Kafka**: Bootstrap servers, consumer group, producer settings
- **JPA**: Hibernate configuration

### Environment Variables

- `SPRING_DATASOURCE_URL`: Database URL (default: `jdbc:mysql://localhost:3310/notification_db`)
- `SPRING_DATASOURCE_USERNAME`: Database username (default: `root`)
- `SPRING_DATASOURCE_PASSWORD`: Database password (default: `rootpassword`)
- `SPRING_KAFKA_BOOTSTRAP_SERVERS`: Kafka bootstrap servers (default: `localhost:9092`)

## Running the Service

### Prerequisites

1. **Infrastructure**: Kafka and Notification DB must be running
   ```bash
   cd implementation
   docker-compose up -d
   ```

2. **Shipping Service**: Should be running and publishing shipping events

### Build and Run

```bash
# Build
./gradlew build

# Run
./gradlew bootRun
```

The service will start on port `8084`.

## Testing

### Unit Tests
```bash
./gradlew test
```

### Integration Tests
Integration tests use Testcontainers for Kafka and MySQL.

## Monitoring

- **Application Logs**: Check logs for notification sending
- **Kafka Topics**: Monitor `notifications` topic for notification events
- **Database**: Check `notifications` and `idempotency_keys` tables
- **Notification Status**: Monitor notification status (SENT, FAILED)

## Email and SMS Services

The service includes **mock email and SMS services** for testing:
- **Email Service**: 95% success rate, 5% failure rate
- **SMS Service**: 90% success rate, 10% failure rate
- Simulated processing delays (50-150ms for email, 30-100ms for SMS)

In production, replace `EmailService` and `SmsService` with integrations to real services:
- **Email**: SendGrid, AWS SES, Mailgun, etc.
- **SMS**: Twilio, AWS SNS, etc.

## Customer Contact Information

The service uses mock customer contact information:
- **Email**: `customer-{customerId}@example.com`
- **Phone**: `+1-555-{last4digits}`

In production, this should be retrieved from a customer database or service.

## Troubleshooting

### Notifications Not Sending

1. Check if Shipping Service is publishing events
2. Verify consumer group is consuming from `shipping` topic
3. Check application logs for errors
4. Review email/SMS service logs

### Email/SMS Service Errors

1. Check email/SMS service client logs
2. Verify email/SMS service configuration
3. Review failure reasons in notifications table

### Duplicate Processing

1. Verify idempotency keys table is working
2. Check if event IDs are unique
3. Review idempotency check logic

