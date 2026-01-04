# Troubleshooting Guide

## Issue: Schema Registration Error with OrderPlacedEventV2

### Problem
When trying to send an order with discount, you get:
```
Error registering Avro schema{"type":"record","name":"OrderPlacedEventV2"...}
```

### Root Cause
1. **Schema Name Mismatch**: If the schema name is `OrderPlacedEventV2` instead of `OrderPlacedEvent`, Schema Registry treats it as a different schema
2. **Compatibility Check**: Schema Registry may be rejecting the new schema due to compatibility rules
3. **Cached Generated Classes**: The Java classes may have been generated with the old schema name

### Solution

#### Step 1: Verify Schema Name
Both schema files should have the same name `OrderPlacedEvent`:

```bash
# Check V1 schema
cat src/main/avro/OrderPlacedEvent.avsc | grep '"name"'

# Check V2 schema  
cat src/main/avro/OrderPlacedEventV2.avsc | grep '"name"'
```

Both should show: `"name": "OrderPlacedEvent"`

#### Step 2: Clean and Rebuild
```bash
./gradlew clean build
```

This will regenerate the Avro classes with the correct schema.

#### Step 3: Check Compatibility Level
```bash
# Check current compatibility level
curl http://localhost:8081/config/orders-avro-value

# If it's too strict, set it to BACKWARD (allows adding optional fields)
curl -X PUT http://localhost:8081/config/orders-avro-value \
  -H "Content-Type: application/vnd.schemaregistry.v1+json" \
  -d '{"compatibility": "BACKWARD"}'
```

#### Step 4: Manually Register the Evolved Schema (Optional)
If automatic registration fails, register it manually:

```bash
curl -X POST http://localhost:8081/subjects/orders-avro-value/versions \
  -H "Content-Type: application/vnd.schemaregistry.v1+json" \
  -d '{
    "schema": "{\"type\":\"record\",\"name\":\"OrderPlacedEvent\",\"namespace\":\"in.codefarm.schema.avro.event\",\"fields\":[{\"name\":\"orderId\",\"type\":\"string\"},{\"name\":\"customerId\",\"type\":\"string\"},{\"name\":\"productId\",\"type\":\"string\"},{\"name\":\"quantity\",\"type\":\"int\"},{\"name\":\"totalAmount\",\"type\":\"double\"},{\"name\":\"orderDate\",\"type\":\"string\"},{\"name\":\"discount\",\"type\":[\"null\",\"double\"],\"default\":null}]}"
  }'
```

#### Step 5: Restart Application
```bash
./gradlew bootRun
```

### Alternative: Use Single Schema File

If you continue having issues, you can use a single schema file approach:

1. **Delete** `OrderPlacedEventV2.avsc`
2. **Update** `OrderPlacedEvent.avsc` to include the discount field
3. **Use** the same `OrderPlacedEvent` class everywhere
4. **Set** discount field when needed (it's optional, so null is fine for old messages)

This is actually the recommended approach for schema evolution - evolve the same schema rather than creating separate schemas.

## Common Issues

### Issue: "Schema being registered is incompatible"

**Solution**: Check compatibility level and ensure the new schema follows compatibility rules:
- Adding optional fields with defaults: ✅ BACKWARD compatible
- Removing required fields: ❌ Not BACKWARD compatible
- Changing field types: ❌ Not compatible

### Issue: "Class OrderPlacedEvent already exists"

**Solution**: This happens when both schema files generate the same class name. Options:
1. Use only one schema file (recommended)
2. Use different namespaces
3. Delete one of the generated classes (not recommended)

### Issue: Schema Registry Connection Refused

**Solution**:
```bash
# Check if Schema Registry is running
curl http://localhost:8081/subjects

# Restart if needed
docker-compose restart schema-registry
```

### Issue: Generated Classes Not Found

**Solution**:
```bash
# Clean and rebuild
./gradlew clean build

# Check generated classes
ls -la build/generated/sources/avro/main/java/in/codefarm/schema/avro/event/
```

