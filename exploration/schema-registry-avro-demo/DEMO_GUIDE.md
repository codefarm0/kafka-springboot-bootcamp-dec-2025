# Schema Registry and Avro Demo Guide

This guide provides a comprehensive walkthrough for demonstrating Schema Registry and Avro features.

## Quick Start

### 1. Start Infrastructure
```bash
docker-compose up -d
```

### 2. Create Topic
```bash
docker exec -it kafka kafka-topics.sh --create \
  --bootstrap-server localhost:9092 \
  --replication-factor 1 \
  --partitions 3 \
  --topic orders-avro
```

### 3. Build and Run
```bash
./gradlew build
./gradlew bootRun
```

### 4. Send Test Message
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

## Demo Flow

### Part 1: Basic Avro Serialization (5 minutes)

**Objective**: Show basic Avro serialization and Schema Registry integration.

**Steps**:
1. Start application
2. Send order event
3. Show schema registration in Schema Registry
4. Show message in Kafdrop (binary format)
5. Show consumer logs (deserialization)

**Talking Points**:
- "Avro uses binary format (smaller than JSON)"
- "Schema is automatically registered with Schema Registry"
- "Only schema ID (4 bytes) in message, not full schema"
- "Type-safe serialization/deserialization"

### Part 2: Schema Registry REST API (3 minutes)

**Objective**: Demonstrate Schema Registry management via REST API.

**Steps**:
1. List all subjects
2. Get schema versions
3. Get specific schema version
4. Check compatibility

**Talking Points**:
- "Schema Registry provides REST API for schema management"
- "Can query schemas, versions, and compatibility"
- "Useful for schema governance and monitoring"

### Part 3: Schema Evolution (7 minutes)

**Objective**: Demonstrate backward compatibility with schema evolution.

**Steps**:
1. Show current schema (version 1)
2. Register new schema with optional field (version 2)
3. Verify compatibility
4. Show old consumer still works
5. Show new consumer can read both versions

**Talking Points**:
- "Adding optional field with default is backward compatible"
- "Old consumers ignore new fields"
- "New consumers can handle both old and new messages"
- "This is the power of schema evolution"

### Part 4: Type Safety (3 minutes)

**Objective**: Demonstrate Avro's type safety.

**Steps**:
1. Show valid message (works)
2. Explain type enforcement
3. Compare with JSON (no type enforcement)

**Talking Points**:
- "Avro enforces types at serialization time"
- "No confusion between string '123' and int 123"
- "Compile-time and runtime validation"
- "Type safety prevents bugs"

## Key Concepts to Explain

### 1. Schema Registry
- Centralized schema management
- Schema versioning
- Compatibility checking
- REST API for management

### 2. Avro Serialization
- Binary format (compact)
- Schema ID in message (4 bytes)
- Type-safe
- Fast serialization/deserialization

### 3. Schema Evolution
- Backward compatibility: Old consumers read new data
- Forward compatibility: New consumers read old data
- Full compatibility: Any version reads any version
- Compatibility rules prevent breaking changes

### 4. Benefits
- Type safety
- Smaller messages
- Schema evolution
- Centralized management
- Performance

## Common Questions

### Q: Why use Avro instead of JSON?
**A**: 
- Binary format (smaller)
- Type safety
- Schema evolution
- Better performance
- Centralized schema management

### Q: What happens if schema changes?
**A**: 
- Schema Registry checks compatibility
- Backward compatible changes are allowed
- Breaking changes are rejected (if compatibility enabled)
- Old consumers can still read new messages

### Q: How is schema stored?
**A**: 
- Schema is stored in Schema Registry
- Only schema ID (4 bytes) in Kafka message
- Consumer fetches schema from registry using ID
- This keeps messages small

### Q: What if Schema Registry is down?
**A**: 
- Producers can cache schemas
- Consumers can cache schemas
- But new schema registration requires registry
- Best practice: High availability for Schema Registry

## Troubleshooting Tips

### Schema Registry Not Available
- Check: `curl http://localhost:8081/subjects`
- Restart: `docker-compose restart schema-registry`

### Schema Not Found
- Send a message first (auto-registers)
- Or register manually via REST API

### Compatibility Error
- Check compatibility level
- Ensure new schema follows rules
- Use BACKWARD or FULL compatibility

## Next Steps

- Test schema evolution scenarios
- Compare message sizes (Avro vs JSON)
- Monitor Schema Registry metrics
- Implement schema versioning strategy

