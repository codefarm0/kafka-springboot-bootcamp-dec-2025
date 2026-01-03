# Kafka Streams Fundamentals

This project demonstrates Kafka Streams fundamentals including stateless transformations (map, filter, branch) for processing order events in real-time.

Check below 2 stories to understand the deep dive about streas - 
* [Kafka Streams Fundamental](https://codefarm0.medium.com/day-11-kafka-streams-fundamentals-day-1-2f2dc1d51016?postPublishedType=initial)
* Upcoming
## Features

- **Stream Processing**: Process order events using Kafka Streams
- **Stateless Transformations**: Map, filter, and branch operations
- **Event Enrichment**: Enrich orders with category and processing metadata
- **Topic Routing**: Route high-value and regular orders to different topics

## Prerequisites

- Java 21
- Docker and Docker Compose
- Gradle

## Setup

### 1. Start Kafka

```bash
docker-compose up -d
```

### 2. Create Topics

```bash
# Input topic
docker exec -it broker1 ./opt/kafka/bin/kafka-topics.sh --create \
  --bootstrap-server localhost:9092 \
  --replication-factor 2 \
  --partitions 3 \
  --topic orders

# Output topics
docker exec -it broker1 ./opt/kafka/bin/kafka-topics.sh --create \
  --bootstrap-server localhost:9092 \
  --replication-factor 2 \
  --partitions 3 \
  --topic high-value-orders

docker exec -it broker1 ./opt/kafka/bin/kafka-topics.sh --create \
  --bootstrap-server localhost:9092 \
  --replication-factor 2 \
  --partitions 3 \
  --topic processed-orders
```

### 3. Run the Application

```bash
./gradlew bootRun
```

## Testing

### Send Test Orders

**High-value order (> $100):**
```bash
curl -X POST http://localhost:8089/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "customer-123",
    "productId": "product-456",
    "quantity": 2,
    "totalAmount": 150.00
  }'
```

**Regular order (<= $100):**
```bash
curl -X POST http://localhost:8089/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "customer-456",
    "productId": "product-789",
    "quantity": 1,
    "totalAmount": 50.00
  }'
```

### Check Output Topics

**High-value orders:**
```bash
docker exec -it broker1 ./opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic high-value-orders \
  --from-beginning
```

**Processed orders:**
```bash
docker exec -it broker1 ./opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic processed-orders \
  --from-beginning
```

## Stream Processing Flow

1. **Input**: Orders are published to `orders` topic
2. **Map**: Orders are enriched with category and processing metadata
3. **Filter**: Invalid orders are filtered out
4. **Branch**: Orders are split into high-value (>= $100) and regular (< $100)
5. **Output**: 
   - High-value orders → `high-value-orders` topic
   - Regular orders → `processed-orders` topic

## Architecture

- **OrderController**: REST API to place orders
- **OrderEventProducer**: Publishes OrderPlacedEvent to Kafka
- **OrderStreamProcessor**: Kafka Streams processor with transformations
- **EnrichedOrderEvent**: Enhanced event with category and processing info

## Configuration

- **Application ID**: `order-streams-app`
- **Server Port**: `8089`
- **Kafka Bootstrap**: `localhost:9092`
- **High-Value Threshold**: $100

## Monitoring

Access Kafdrop UI at: http://localhost:9000

