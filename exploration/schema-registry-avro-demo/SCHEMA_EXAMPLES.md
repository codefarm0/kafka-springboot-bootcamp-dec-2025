# Schema Registry API Examples

This document provides complete, ready-to-use examples for Schema Registry REST API calls.

## Original Schema (Version 1)

**File**: `src/main/avro/OrderPlacedEvent.avsc`

**JSON Schema**:
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
    {"name": "orderDate", "type": "string"}
  ]
}
```

**Escaped JSON (for curl)**:
```json
{"type":"record","name":"OrderPlacedEvent","namespace":"in.codefarm.schema.avro.event","fields":[{"name":"orderId","type":"string"},{"name":"customerId","type":"string"},{"name":"productId","type":"string"},{"name":"quantity","type":"int"},{"name":"totalAmount","type":"double"},{"name":"orderDate","type":"string"}]}
```

## Evolved Schema (Version 2 - with discount)

**File**: `src/main/avro/OrderPlacedEventV2.avsc`

**JSON Schema**:
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

**Escaped JSON (for curl)**:
```json
{"type":"record","name":"OrderPlacedEvent","namespace":"in.codefarm.schema.avro.event","fields":[{"name":"orderId","type":"string"},{"name":"customerId","type":"string"},{"name":"productId","type":"string"},{"name":"quantity","type":"int"},{"name":"totalAmount","type":"double"},{"name":"orderDate","type":"string"},{"name":"discount","type":["null","double"],"default":null}]}
```

## API Examples

### 1. List All Subjects

```bash
curl http://localhost:8081/subjects
```

**Response**:
```json
["orders-avro-value"]
```

### 2. Get Schema Versions for a Subject

```bash
curl http://localhost:8081/subjects/orders-avro-value/versions
```

**Response**:
```json
[1, 2]
```

### 3. Get Specific Schema Version

```bash
curl http://localhost:8081/subjects/orders-avro-value/versions/1
```

**Response**:
```json
{
  "subject": "orders-avro-value",
  "version": 1,
  "id": 1,
  "schema": "{\"type\":\"record\",\"name\":\"OrderPlacedEvent\",\"namespace\":\"in.codefarm.schema.avro.event\",\"fields\":[{\"name\":\"orderId\",\"type\":\"string\"},{\"name\":\"customerId\",\"type\":\"string\"},{\"name\":\"productId\",\"type\":\"string\"},{\"name\":\"quantity\",\"type\":\"int\"},{\"name\":\"totalAmount\",\"type\":\"double\"},{\"name\":\"orderDate\",\"type\":\"string\"}]}"
}
```

### 4. Get Latest Schema Version

```bash
curl http://localhost:8081/subjects/orders-avro-value/versions/latest
```

### 5. Register New Schema Version

**Register Version 1 (Original)**:
```bash
curl -X POST http://localhost:8081/subjects/orders-avro-value/versions \
  -H "Content-Type: application/vnd.schemaregistry.v1+json" \
  -d '{
    "schema": "{\"type\":\"record\",\"name\":\"OrderPlacedEvent\",\"namespace\":\"in.codefarm.schema.avro.event\",\"fields\":[{\"name\":\"orderId\",\"type\":\"string\"},{\"name\":\"customerId\",\"type\":\"string\"},{\"name\":\"productId\",\"type\":\"string\"},{\"name\":\"quantity\",\"type\":\"int\"},{\"name\":\"totalAmount\",\"type\":\"double\"},{\"name\":\"orderDate\",\"type\":\"string\"}]}"
  }'
```

**Register Version 2 (with discount - Backward Compatible)**:
```bash
curl -X POST http://localhost:8081/subjects/orders-avro-value/versions \
  -H "Content-Type: application/vnd.schemaregistry.v1+json" \
  -d '{
    "schema": "{\"type\":\"record\",\"name\":\"OrderPlacedEvent\",\"namespace\":\"in.codefarm.schema.avro.event\",\"fields\":[{\"name\":\"orderId\",\"type\":\"string\"},{\"name\":\"customerId\",\"type\":\"string\"},{\"name\":\"productId\",\"type\":\"string\"},{\"name\":\"quantity\",\"type\":\"int\"},{\"name\":\"totalAmount\",\"type\":\"double\"},{\"name\":\"orderDate\",\"type\":\"string\"},{\"name\":\"discount\",\"type\":[\"null\",\"double\"],\"default\":null}]}"
  }'
```

**Response**:
```json
{"id": 2}
```

### 6. Check Compatibility

**Check if new schema is compatible with latest version**:
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

### 7. Get Compatibility Level

```bash
curl http://localhost:8081/config/orders-avro-value
```

**Response**:
```json
{"compatibilityLevel": "BACKWARD"}
```

### 8. Set Compatibility Level

**Set to BACKWARD**:
```bash
curl -X PUT http://localhost:8081/config/orders-avro-value \
  -H "Content-Type: application/vnd.schemaregistry.v1+json" \
  -d '{"compatibility": "BACKWARD"}'
```

**Set to FORWARD**:
```bash
curl -X PUT http://localhost:8081/config/orders-avro-value \
  -H "Content-Type: application/vnd.schemaregistry.v1+json" \
  -d '{"compatibility": "FORWARD"}'
```

**Set to FULL**:
```bash
curl -X PUT http://localhost:8081/config/orders-avro-value \
  -H "Content-Type: application/vnd.schemaregistry.v1+json" \
  -d '{"compatibility": "FULL"}'
```

**Set to NONE**:
```bash
curl -X PUT http://localhost:8081/config/orders-avro-value \
  -H "Content-Type: application/vnd.schemaregistry.v1+json" \
  -d '{"compatibility": "NONE"}'
```

## Using jq for Better Output

If you have `jq` installed, you can format JSON responses:

```bash
curl http://localhost:8081/subjects/orders-avro-value/versions/1 | jq
```

## Helper Script

Create a file `check-compatibility.sh`:

```bash
#!/bin/bash

SCHEMA='{"type":"record","name":"OrderPlacedEvent","namespace":"in.codefarm.schema.avro.event","fields":[{"name":"orderId","type":"string"},{"name":"customerId","type":"string"},{"name":"productId","type":"string"},{"name":"quantity","type":"int"},{"name":"totalAmount","type":"double"},{"name":"orderDate","type":"string"},{"name":"discount","type":["null","double"],"default":null}]}'

curl -X POST http://localhost:8081/compatibility/subjects/orders-avro-value/versions/latest \
  -H "Content-Type: application/vnd.schemaregistry.v1+json" \
  -d "{\"schema\": \"$SCHEMA\"}" | jq
```

Make it executable:
```bash
chmod +x check-compatibility.sh
./check-compatibility.sh
```

