# k6 Load Test – Orders API

This load test simulates traffic for the **Orders API** with two order types based on business rules.

* **Regular orders** → `totalAmount ≤ 100`
* **High-value orders** → `totalAmount > 100`

---

## 0. Install k6

### macOS

```bash
brew install k6
```

### Linux (Debian / Ubuntu)

```bash
sudo apt update
sudo apt install k6
```

### Windows (Chocolatey)

```bash
choco install k6
```

Verify installation:

```bash
k6 version
```

---

## 1. Scenarios Configuration

```js
export const options = {
  scenarios: {
    regular_orders: { ... },
    high_value_orders: { ... }
  }
}
```

Defines **two independent load scenarios**, each targeting a different business path.

---

## 2. Virtual Users (VUs)

```js
regular_orders: {
  vus: 10,
  duration: '1m'
}

high_value_orders: {
  vus: 5,
  duration: '1m'
}
```

* **Regular Orders**

    * 10 concurrent virtual users
* **High-Value Orders**

    * 5 concurrent virtual users

Each VU runs the scenario function in a loop for the duration.

---

## 3. Requests per Minute (RPM)

Each function has:

```js
sleep(1);
```

That means **one request per VU per second**.

### Approximate Load Generated

* **Regular orders**

    * 10 VUs × 60 sec ≈ **600 requests/min**
* **High-value orders**

    * 5 VUs × 60 sec ≈ **300 requests/min**

### Total Load

➡ **~900 requests per minute**

> ⚠️ Actual RPM may be slightly lower due to network latency and response time.

---

## 4. Executor Choice

```js
executor: 'constant-vus'
```

* Maintains steady concurrency
* Ideal for:

    * Baseline performance testing
    * SLA validation
    * Capacity checks

---

## 5. Business Flow Mapping

```js
exec: 'regularOrders'
exec: 'highValueOrders'
```

Each scenario maps directly to a function, ensuring:

* Clear separation of business paths
* Easier debugging and tuning

---

## 6. Thresholds (Performance Guardrails)

```js
thresholds: {
  http_req_failed: ['rate<0.01'],
  http_req_duration: ['p(95)<500'],
}
```

* Less than **1% request failures**
* **95%** of requests must complete within **500ms**

If thresholds fail, the test fails — no false positives.

---

## 7. How to Tune Load

### Increase Throughput (RPM)

* Increase `vus`
* Reduce `sleep()` time

Example:

```js
sleep(0.5); // doubles RPM
```

---

### Simulate Realistic Traffic Mix

Adjust VU ratio:

```js
regular_orders: 20 VUs
high_value_orders: 5 VUs
```

This simulates:

* 80% regular traffic
* 20% high-value traffic

---

### Simulate Spikes

Switch executor:

```js
executor: 'ramping-vus'
```

Useful for:

* Sale events
* Flash traffic
* Incident simulations

---

### Control Exact RPM

Use:

```js
executor: 'constant-arrival-rate'
```

Best when:

* You need precise throughput
* Backpressure behavior matters

---

## 8. Running the Test

```bash
k6 run orders-load-test.js
```

---

## Summary

This test setup:

* Models **real business bifurcation**
* Provides **predictable load**
* Is easy to tune for growth
* Works well with monitoring and SLOs

Small script. Serious signal.
