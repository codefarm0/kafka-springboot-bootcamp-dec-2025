# Saga Pattern Test Scenarios

## Scenario 1: Payment Failure
**Description**: Payment fails due to amount exceeding limit (> 1000)

**Expected Flow**:
1. OrderPlaced → PaymentProcessed (FAILED) → OrderCancelled (compensation)

**Curl Command**:
```bash
curl -X POST http://localhost:8088/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "customer-123",
    "productId": "product-456",
    "quantity": 2,
    "totalAmount": 1500.00
  }'
```

---

## Scenario 2: Inventory Reservation Failure
**Description**: Payment succeeds but inventory reservation fails (amount > 500)

**Expected Flow**:
1. OrderPlaced → PaymentProcessed (SUCCESS) → InventoryReserved (UNAVAILABLE) → PaymentRefunded → OrderCancelled (compensation)

**Curl Command**:
```bash
curl -X POST http://localhost:8088/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "customer-123",
    "productId": "product-456",
    "quantity": 2,
    "totalAmount": 600.00
  }'
```

---

## Scenario 3: Successful Order Flow
**Description**: Complete successful saga execution

**Expected Flow**:
1. OrderPlaced → PaymentProcessed (SUCCESS) → InventoryReserved (RESERVED) → Email sent

**Curl Command**:
```bash
curl -X POST http://localhost:8088/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "customer-123",
    "productId": "product-456",
    "quantity": 2,
    "totalAmount": 99.99
  }'
```

---

## Notes:
- **Payment Failure Threshold**: Amount > 1000
- **Inventory Failure Threshold**: Amount > 500
- **Success Threshold**: Amount <= 500

