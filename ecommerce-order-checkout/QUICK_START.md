# Quick Start Guide

This guide will help you get the Order Service up and running quickly.

## Prerequisites

- Docker and Docker Compose installed
- Java 25 (or compatible version)
- Gradle (or use Gradle wrapper)

## Step 1: Start Infrastructure

Start all infrastructure services (Kafka, MySQL, Kafka Connect, Debezium, Kafdrop):

```bash
cd implementation
docker-compose up -d
```

Wait for all services to be healthy (check with `docker-compose ps`).

## Step 2: Deploy Debezium Connector

Deploy the Debezium MySQL connector with CloudEvents format:

```bash
cd implementation
./scripts/deploy-debezium-connector.sh
```

Verify connector is running:
```bash
curl http://localhost:8083/connectors/order-service-connector/status
```

## Step 3: Verify Database Schema

The database schema should be automatically created when MySQL starts (via `init-order-db.sql`).

Verify tables exist:
```bash
docker exec -it order-db mysql -uroot -prootpassword order_db -e "SHOW TABLES;"
```

You should see:
- `orders`
- `order_items`
- `outbox_events`

## Step 4: Build and Run Order Service

```bash
cd order-service
./gradlew build
./gradlew bootRun
```

The service will start on `http://localhost:8080`.

## Step 5: Test Order Creation

Create a test order:

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "customer-123",
    "items": [
      {
        "productId": "product-1",
        "productName": "Test Product",
        "quantity": 2,
        "unitPrice": 29.99
      }
    ],
    "shippingAddress": "123 Main St, City, Country"
  }'
```

## Step 6: Verify Event Published

### Check Kafka Topic

1. Open Kafdrop UI: http://localhost:9000
2. Look for topic: `com.ecommerce.order.placed`
3. Verify the event is in CloudEvents format

### Check Database

```bash
docker exec -it order-db mysql -uroot -prootpassword order_db \
  -e "SELECT * FROM outbox_events ORDER BY created_at DESC LIMIT 1;"
```

### Check Order

```bash
curl http://localhost:8080/api/orders/{orderId}
```

## Monitoring

- **Kafdrop**: http://localhost:9000 (Kafka UI)
- **Kafka Connect**: http://localhost:8083 (Connector management)
- **Order Service**: http://localhost:8080 (REST API)

## Troubleshooting

### Services Not Starting

```bash
# Check logs
docker-compose logs kafka
docker-compose logs order-db
docker-compose logs kafka-connect

# Restart services
docker-compose restart
```

### Debezium Connector Issues

```bash
# Check connector status
curl http://localhost:8083/connectors/order-service-connector/status

# Check connector config
curl http://localhost:8083/connectors/order-service-connector/config

# Restart connector
curl -X POST http://localhost:8083/connectors/order-service-connector/restart
```

### Order Service Not Starting

1. Check database connection in `application.yml`
2. Verify Kafka is accessible
3. Check application logs

## Next Steps

Once Order Service is working:

1. Implement **Payment Service** (next in sequence)
2. Implement **Inventory Service**
3. Implement **Shipping Service**
4. Implement **Notification Service**

See [Implementation README](README.md) for the complete implementation plan.

