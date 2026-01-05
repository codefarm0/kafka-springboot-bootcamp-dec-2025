# Schema Registry and Avro Demo

This project demonstrates Schema Registry and Avro serialization with Kafka, including schema evolution and compatibility rules.
**Get the deep dive story here** - [Schema Registry + Avro + Versioning ](https://codefarm0.medium.com/schema-registry-avro-versioning-day-13-4a4b7fe8700c) 

## Overview

This project shows:
- **Avro serialization**: Binary format, smaller than JSON
- **Schema Registry**: Centralized schema management
- **Schema evolution**: Backward/forward compatibility
- **Type safety**: Enforced at serialization/deserialization

## Architecture

```
Order Controller → OrderEventProducer → Kafka (Avro) → OrderEventConsumer
                      ↓
                 Schema Registry
                 (Schema Management)
```

## Prerequisites

- Docker and Docker Compose
- Java 21
- Gradle

## Setup

### Step 1: Start Infrastructure

```bash
cd exploration/schema-registry-avro-demo
docker-compose up -d
```

This starts:
- Kafka (KRaft mode, port 9092) - No Zookeeper needed!
- Schema Registry (port 8081)
- Kafdrop (port 9000) - Kafka UI

### Step 2: Create Kafka Topic

```bash
docker exec -it kafka kafka-topics.sh --create \
  --bootstrap-server localhost:9092 \
  --replication-factor 1 \
  --partitions 3 \
  --topic orders-avro
```

### Step 3: Generate Avro Classes

```bash
./gradlew build
```

This will:
- Compile Avro schemas from `src/main/avro/`
- Generate Java classes in `build/generated/sources/avro/`

### Step 4: Start Application

```bash
./gradlew bootRun
```

## Usage

### Send Order (Avro)

**Basic Order (Version 1 schema)**:
```bash
curl -X POST http://localhost:8090/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "customer-123",
    "productId": "product-456",
    "quantity": 2,
    "totalAmount": 99.99
  }'
```

**Order with Discount (Version 2 schema - demonstrates schema evolution)**:
```bash
curl -X POST http://localhost:8090/api/orders/with-discount \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "customer-123",
    "productId": "product-456",
    "quantity": 2,
    "totalAmount": 99.99,
    "discount": 10.0
  }'
```

### Verify Schema Registration

```bash
# List all subjects (schemas)
curl http://localhost:8081/subjects

# Get schema for orders-avro-value
curl http://localhost:8081/subjects/orders-avro-value/versions

# Get specific schema version
curl http://localhost:8081/subjects/orders-avro-value/versions/1
```

### View Messages in Kafdrop

1. Open http://localhost:9000
2. Select `orders-avro` topic
3. View messages (they'll be in binary Avro format)

## Schema Evolution Demo

### Step 1: Register Original Schema (Version 1)

The schema is automatically registered when you first send a message.

### Step 2: Add Optional Field (Version 2 - Backward Compatible)

1. Update schema to include `discount` field (optional with default)
2. Register new schema version
3. Old consumers can still read new messages (they ignore discount)

### Step 3: Test Compatibility

```bash
# Check compatibility
curl -X POST http://localhost:8081/compatibility/subjects/orders-avro-value/versions/latest \
  -H "Content-Type: application/json" \
  -d '{
    "schema": "{...new schema...}"
  }'
```

## Key Features

### 1. Automatic Schema Registration

- Schema is registered automatically on first message send
- Subject name: `{topic-name}-value` (e.g., `orders-avro-value`)
- Schema ID is embedded in message (4 bytes)

### 2. Type Safety

- Avro enforces types at serialization time
- No type confusion (string "123" vs int 123)
- Compile-time validation

### 3. Schema Evolution

- Backward compatible: Old consumers read new data
- Forward compatible: New consumers read old data
- Full compatible: Any version reads any version

### 4. Compact Messages

- Binary format (smaller than JSON)
- Only schema ID in message (not full schema)
- Better performance

## Project Structure

```
src/main/
├── avro/                          # Avro schema definitions
│   ├── OrderPlacedEvent.avsc      # Version 1 schema
│   └── OrderPlacedEventV2.avsc    # Version 2 schema (with discount)
├── java/in/codefarm/schema/avro/
│   ├── config/
│   │   └── KafkaAvroConfig.java   # Avro producer/consumer configuration
│   ├── controller/
│   │   └── OrderController.java   # REST endpoints
│   ├── service/
│   │   └── OrderEventProducer.java # Avro producer
│   └── consumer/
│       └── OrderEventConsumer.java # Avro consumer
└── resources/
    └── application.properties     # Configuration
```

## Configuration

### Schema Registry URL

```properties
spring.kafka.producer.properties.schema.registry.url=http://localhost:8081
spring.kafka.consumer.properties.schema.registry.url=http://localhost:8081
```

### Avro Serializers

```properties
spring.kafka.producer.value-serializer=io.confluent.kafka.serializers.KafkaAvroSerializer
spring.kafka.consumer.value-deserializer=io.confluent.kafka.serializers.KafkaAvroDeserializer
```

## Troubleshooting

### Schema Registry Not Available

**Error**: `Connection refused` or `Schema registry not available`

**Solution**:
```bash
# Check if Schema Registry is running
curl http://localhost:8081/subjects

# Restart if needed
docker-compose restart schema-registry
```

### Schema Not Found

**Error**: `Schema not found`

**Solution**:
- Send a message first (schema auto-registers)
- Or manually register schema via REST API

### Deserialization Error

**Error**: `Could not deserialize Avro message`

**Solution**:
- Verify schema is registered in Schema Registry
- Check `specific.avro.reader=true` in consumer config
- Ensure consumer uses correct schema version

## Next Steps

- Test schema evolution (add/remove fields)
- Test compatibility rules (backward/forward)
- Compare message sizes (Avro vs JSON)
- Monitor Schema Registry via REST API

