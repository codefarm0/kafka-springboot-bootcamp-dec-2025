# Schema Registry and Avro Demo Scenarios

This document provides step-by-step scenarios for demonstrating Schema Registry and Avro features.

## Scenario 1: Basic Avro Serialization

### Objective
Demonstrate basic Avro serialization and Schema Registry integration.

### Steps

1. **Start Infrastructure**
```bash
docker-compose up -d
```

2. **Create Topic**
```bash
docker exec -it kafka kafka-topics.sh --create \
  --bootstrap-server localhost:9092 \
  --replication-factor 1 \
  --partitions 3 \
  --topic orders-avro
```

3. **Start Application**
```bash
./gradlew bootRun
```

4. **Send Order Event**
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

5. **Verify Schema Registration**
```bash
# List all subjects
curl http://localhost:8081/subjects

# Should return: ["orders-avro-value"]

# Get schema versions
curl http://localhost:8081/subjects/orders-avro-value/versions

# Get schema version 1
curl http://localhost:8081/subjects/orders-avro-value/versions/1
```

6. **View Message in Kafdrop**
- Open http://localhost:9000
- Select `orders-avro` topic
- View messages (binary Avro format)

### Expected Results
- ✅ Schema automatically registered in Schema Registry
- ✅ Message sent to Kafka in Avro binary format
- ✅ Consumer receives and deserializes message
- ✅ Logs show Avro serialization/deserialization

### Demo Talking Points
- "Schema is automatically registered on first message send"
- "Messages are binary-encoded (smaller than JSON)"
- "Schema ID (4 bytes) is embedded in message"
- "Consumer fetches schema from Schema Registry using schema ID"

---

## Scenario 2: Schema Evolution - Adding Optional Field

### Objective
Demonstrate backward compatibility by adding an optional field.

### Steps

1. **Register New Schema Version (with discount field)**

First, let's check the current schema:
```bash
curl http://localhost:8081/subjects/orders-avro-value/versions/1
```

2. **Register New Schema Version (with discount field)**

First, get the current schema to see what we're evolving from:
```bash
curl http://localhost:8081/subjects/orders-avro-value/versions/1
```

Now register the new schema version with discount field:
```bash
curl -X POST http://localhost:8081/subjects/orders-avro-value/versions \
  -H "Content-Type: application/vnd.schemaregistry.v1+json" \
  -d '{
    "schema": "{\"type\":\"record\",\"name\":\"OrderPlacedEvent\",\"namespace\":\"in.codefarm.schema.avro.event\",\"fields\":[{\"name\":\"orderId\",\"type\":\"string\"},{\"name\":\"customerId\",\"type\":\"string\"},{\"name\":\"productId\",\"type\":\"string\"},{\"name\":\"quantity\",\"type\":\"int\"},{\"name\":\"totalAmount\",\"type\":\"double\"},{\"name\":\"orderDate\",\"type\":\"string\"},{\"name\":\"discount\",\"type\":[\"null\",\"double\"],\"default\":null}]}"
  }'
```

**Complete Schema JSON (formatted for readability)**:
```json
{
  "type": "record",
  "name": "OrderPlacedEvent",
  "namespace": "in.codefarm.schema.avro.event",
  "fields": [
    {"name": "orderId", "type": "string"},
    {"name": "customerId", "type": "string"},
    {"name": "productId", "type": "string"},
    {"name": "quantity", "type": "int"},
    {"name": "totalAmount", "type": "double"},
    {"name": "orderDate", "type": "string"},
    {"name": "discount", "type": ["null", "double"], "default": null}
  ]
}
```

3. **Verify Compatibility**

Check if the new schema is compatible with the latest version:
```bash
curl -X POST http://localhost:8081/compatibility/subjects/orders-avro-value/versions/latest \
  -H "Content-Type: application/vnd.schemaregistry.v1+json" \
  -d '{
    "schema": "{\"type\":\"record\",\"name\":\"OrderPlacedEvent\",\"namespace\":\"in.codefarm.schema.avro.event\",\"fields\":[{\"name\":\"orderId\",\"type\":\"string\"},{\"name\":\"customerId\",\"type\":\"string\"},{\"name\":\"productId\",\"type\":\"string\"},{\"name\":\"quantity\",\"type\":\"int\"},{\"name\":\"totalAmount\",\"type\":\"double\"},{\"name\":\"orderDate\",\"type\":\"string\"},{\"name\":\"discount\",\"type\":[\"null\",\"double\"],\"default\":null}]}"
  }'
```

**Response if compatible**:
```json
{"is_compatible": true}
```

**Response if incompatible**:
```json
{
  "error_code": 409,
  "message": "Schema being registered is incompatible with an earlier schema"
}
```

4. **Send Order with New Schema**
```bash
# Note: This requires updating the producer code to use new schema
# For demo, we'll show the concept
```

### Expected Results
- ✅ New schema version registered (version 2)
- ✅ Compatibility check passes (backward compatible)
- ✅ Old consumers can still read new messages (ignore discount field)
- ✅ New consumers can read both old and new messages

### Demo Talking Points
- "Adding optional field with default is backward compatible"
- "Old consumers ignore the new field"
- "New consumers can handle both old and new messages"
- "This is the power of schema evolution"

---

## Scenario 3: Compare Avro vs JSON Message Size

### Objective
Demonstrate that Avro messages are smaller than JSON.

### Steps

1. **Send JSON Message** (if you have a JSON producer)
```bash
# Send to orders-json topic (if exists)
```

2. **Send Avro Message**
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

3. **Compare Message Sizes**
- Check message size in Kafdrop
- Avro: Binary format (smaller)
- JSON: Text format (larger)

### Expected Results
- ✅ Avro messages are smaller (binary format)
- ✅ Only schema ID in message (4 bytes), not full schema
- ✅ Better performance (faster serialization/deserialization)

### Demo Talking Points
- "Avro uses binary format (smaller than JSON)"
- "Only schema ID (4 bytes) in message, not full schema"
- "Better network efficiency"
- "Faster serialization/deserialization"

---

## Scenario 4: Schema Registry REST API

### Objective
Demonstrate Schema Registry REST API operations.

### Steps

1. **List All Subjects**
```bash
curl http://localhost:8081/subjects
```

2. **Get Schema Versions**
```bash
curl http://localhost:8081/subjects/orders-avro-value/versions
```

3. **Get Specific Schema Version**
```bash
curl http://localhost:8081/subjects/orders-avro-value/versions/1
```

4. **Get Latest Schema Version**
```bash
curl http://localhost:8081/subjects/orders-avro-value/versions/latest
```

5. **Check Compatibility**
```bash
curl -X POST http://localhost:8081/compatibility/subjects/orders-avro-value/versions/latest \
  -H "Content-Type: application/vnd.schemaregistry.v1+json" \
  -d '{
    "schema": "{...schema...}"
  }'
```

6. **Get Compatibility Level**
```bash
curl http://localhost:8081/config/orders-avro-value
```

7. **Set Compatibility Level**
```bash
curl -X PUT http://localhost:8081/config/orders-avro-value \
  -H "Content-Type: application/vnd.schemaregistry.v1+json" \
  -d '{"compatibility": "BACKWARD"}'
```

### Expected Results
- ✅ Can list all registered schemas
- ✅ Can get schema versions
- ✅ Can check compatibility
- ✅ Can configure compatibility levels

### Demo Talking Points
- "Schema Registry provides REST API for schema management"
- "Can query schemas, versions, and compatibility"
- "Can configure compatibility levels per subject"
- "Useful for schema governance"

---

## Scenario 5: Type Safety Demonstration

### Objective
Demonstrate Avro's type safety compared to JSON.

### Steps

1. **Send Valid Order**
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

2. **Try to Send Invalid Order** (wrong type)
```bash
# This would fail at serialization time
# Avro enforces types, JSON doesn't
```

### Expected Results
- ✅ Avro enforces types at serialization time
- ✅ Type mismatches cause errors immediately
- ✅ No runtime surprises

### Demo Talking Points
- "Avro enforces types at serialization time"
- "No confusion between string '123' and int 123"
- "Compile-time and runtime validation"
- "Type safety prevents bugs"

---

## Troubleshooting

### Schema Registry Not Available
```bash
# Check if running
curl http://localhost:8081/subjects

# Restart if needed
docker-compose restart schema-registry
```

### Schema Not Found
- Send a message first (auto-registers schema)
- Or manually register via REST API

### Compatibility Error
- Check compatibility level
- Ensure new schema follows compatibility rules
- Use BACKWARD or FULL compatibility for safe evolution

---

## Key Takeaways

1. **Automatic Schema Registration**: Schema registered on first message send
2. **Binary Format**: Smaller messages than JSON
3. **Type Safety**: Enforced at serialization/deserialization
4. **Schema Evolution**: Backward/forward compatibility
5. **Schema Registry**: Centralized schema management
6. **REST API**: Easy schema management and querying

