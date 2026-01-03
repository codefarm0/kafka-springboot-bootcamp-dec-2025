# Order Analytics Service - Day 12 Implementation

This document describes the stateful operations implementation for Day 12 of the Kafka Streams bootcamp.

## Overview

The Order Analytics Service provides real-time aggregations and analytics on order events using Kafka Streams stateful operations.

## Features

1. **Order Count per Customer**: Counts total orders placed by each customer
2. **Total Amount per Customer**: Sums total order amounts for each customer
3. **Windowed Counts**: Counts orders per customer per minute (time-windowed aggregation)

## Architecture

```
orders topic → OrderAnalyticsProcessor → State Stores → REST API
```

### State Stores

- `order-count-store`: Key-Value store for order counts per customer
- `total-amount-store`: Key-Value store for total amounts per customer
- `windowed-count-store`: Window store for time-windowed order counts

## API Endpoints

### 1. Get Order Count for Customer

```bash
GET /api/analytics/count/{customerId}
```

**Example:**
```bash
curl http://localhost:8089/api/analytics/count/customer-123
```

**Response:**
```json
{
  "customerId": "customer-123",
  "count": 5
}
```

### 2. Get Total Amount for Customer

```bash
GET /api/analytics/total/{customerId}
```

**Example:**
```bash
curl http://localhost:8089/api/analytics/total/customer-123
```

**Response:**
```json
{
  "customerId": "customer-123",
  "totalAmount": 499.95
}
```

### 3. Get Windowed Counts for Customer

```bash
GET /api/analytics/windowed/{customerId}?startTime={epochMillis}&endTime={epochMillis}
```

**Example:**
```bash
# Get windowed counts for last hour
curl "http://localhost:8089/api/analytics/windowed/customer-123?startTime=$(($(date +%s) - 3600))000&endTime=$(date +%s)000"
```

**Response:**
```json
[
  {
    "customerId": "customer-123",
    "windowStart": "2026-01-03T10:00:00Z",
    "windowEnd": "2026-01-03T10:01:00Z",
    "count": 3
  },
  {
    "customerId": "customer-123",
    "windowStart": "2026-01-03T10:01:00Z",
    "windowEnd": "2026-01-03T10:02:00Z",
    "count": 2
  }
]
```

### 4. Get All Order Counts

```bash
GET /api/analytics/counts
```

**Example:**
```bash
curl http://localhost:8089/api/analytics/counts
```

**Response:**
```json
{
  "customer-123": 5,
  "customer-456": 3,
  "customer-789": 10
}
```

### 5. Get All Total Amounts

```bash
GET /api/analytics/totals
```

**Example:**
```bash
curl http://localhost:8089/api/analytics/totals
```

**Response:**
```json
{
  "customer-123": 499.95,
  "customer-456": 299.97,
  "customer-789": 999.90
}
```

## Testing the Service

### Step 1: Start Kafka and Application

```bash
# Start Kafka (if using docker-compose)
cd exploration/kafka-streams-fundamentals
docker-compose up -d

# Start the application
./gradlew bootRun
```

### Step 2: Send Test Orders

```bash
# Send orders via the existing order endpoint
curl -X POST http://localhost:8089/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "customer-123",
    "productId": "product-456",
    "quantity": 2,
    "totalAmount": 99.99
  }'

# Send multiple orders to see aggregations
for i in {1..5}; do
  curl -X POST http://localhost:8089/api/orders \
    -H "Content-Type: application/json" \
    -d "{
      \"customerId\": \"customer-123\",
      \"productId\": \"product-456\",
      \"quantity\": $i,
      \"totalAmount\": $((i * 20)).99
    }"
  sleep 1
done
```

### Step 3: Query Analytics

```bash
# Get order count
curl http://localhost:8089/api/analytics/count/customer-123

# Get total amount
curl http://localhost:8089/api/analytics/total/customer-123

# Get all counts
curl http://localhost:8089/api/analytics/counts
```

## Implementation Details

### Processor: OrderAnalyticsProcessor

Located in: `in.codefarm.streams.analytics.processor.OrderAnalyticsProcessor`

**Key Operations:**
1. **Count Aggregation**: Groups orders by customer ID and counts them
2. **Sum Aggregation**: Groups orders by customer ID and sums total amounts
3. **Windowed Count**: Groups orders by customer ID in 1-minute windows and counts them

### State Stores

All state stores are materialized (named) so they can be queried via REST API:

- **order-count-store**: `KTable<String, Long>`
- **total-amount-store**: `KTable<String, BigDecimal>`
- **windowed-count-store**: `KTable<Windowed<String>, Long>`

### Controller: OrderAnalyticsController

Located in: `in.codefarm.streams.analytics.controller.OrderAnalyticsController`

**Features:**
- Accesses state stores via `KafkaStreams.store()`
- Handles service unavailable (503) when streams not running
- Provides error handling for all endpoints

## State Store Location

State stores are persisted in:
```
/tmp/kafka-streams/order-streams-app-{process-id}/
```

## Changelog Topics

Kafka Streams automatically creates changelog topics for state stores:
- `order-streams-app-order-count-store-changelog`
- `order-streams-app-total-amount-store-changelog`
- `order-streams-app-windowed-count-store-changelog`

These topics are used for:
- State recovery on restart
- Replication across instances
- Durability

## Monitoring

### Check State Store Size

State stores grow as more unique customers place orders. Monitor:
- Disk usage in state store directory
- Changelog topic sizes
- Memory usage (RocksDB)

### Check Processing Status

```bash
# Check if streams are running
curl http://localhost:8089/api/analytics/counts

# If returns 503, streams are not running yet
# Check application logs for state transitions
```

## Troubleshooting

### State Store Not Available (503)

**Cause**: Kafka Streams not in RUNNING state

**Solution**: 
- Check application logs for state transitions
- Ensure Kafka brokers are running
- Wait for streams to reach RUNNING state

### No Data in State Store

**Cause**: No orders processed yet or wrong customer ID

**Solution**:
- Send test orders first
- Check customer IDs match
- Verify orders are being processed (check logs)

### Windowed Counts Empty

**Cause**: Time window has expired or no orders in time range

**Solution**:
- Use appropriate time range (last hour, etc.)
- Ensure orders are being sent continuously
- Check window size (1 minute) matches query range

## Next Steps

- Add more aggregations (average, min, max)
- Add more window types (hopping, session)
- Add joins with other streams
- Add metrics and monitoring

