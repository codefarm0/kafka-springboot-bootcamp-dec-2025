# k6 Order API Load Test – Quick README

### What this test does

* Sends **POST** requests to `/api/orders/with-headers`
* Uses **randomized order data**
* Runs a **spike load test** (up to ~100 requests/sec)
* Validates response status and latency (p95)

---

### Prerequisites

* **k6 installed**

  ```bash
  brew install k6        # macOS
  choco install k6      # Windows
  sudo apt install k6   # Linux
  ```
* Order service running on:

  ```
  http://localhost:8080
  ```

---

### How to run

```bash
k6 run order_load_test.js
```

---

### What to watch while it runs

* API response time & error rate
* Database connection usage
* Kafka producer throughput / consumer lag

---

# What this k6 script is doing (Explained)

This script simulates **real users placing orders** on your Order API and measures how your system behaves under **sudden load spikes**.

---

## Imports (what k6 gives us)

```js
import http from 'k6/http';
import { check, sleep } from 'k6';
```

* `http` → lets k6 send HTTP requests
* `check` → validate responses (status, SLA)
* `sleep` → pause between iterations (think time)

---

## Test configuration (`options`)

```js
export const options = {
  scenarios: {
    order_spike_test: {
      executor: 'ramping-arrival-rate',
```

### Why `ramping-arrival-rate`?

* You control **requests per second (RPS)**, not virtual users
* Best for **API + Kafka producer stress**
* Matches real traffic patterns better than fixed users

---

### Load shape

```js
startRate: 5,
stages: [
  { target: 20, duration: '30s' },
  { target: 100, duration: '1m' },
  { target: 100, duration: '1m' },
  { target: 0, duration: '30s' },
],
```

| Phase     | Meaning                                |
| --------- | -------------------------------------- |
| Warm-up   | Slowly introduce traffic               |
| Spike     | Sudden increase (realistic prod event) |
| Sustain   | Hold pressure to expose leaks          |
| Cool-down | Graceful shutdown                      |

---

### Virtual User limits

```js
preAllocatedVUs: 50,
maxVUs: 200,
```

* k6 dynamically adds VUs to maintain RPS
* Prevents under-provisioning during spikes

---

## Thresholds (your SLOs)

```js
thresholds: {
  http_req_failed: ['rate<0.01'],
  http_req_duration: ['p(95)<500'],
},
```

This **automatically fails the test** if:

* ❌ More than **1% requests fail**
* ❌ 95% of requests take **> 500ms**

Think of this as **automated acceptance criteria**

---

##  The test logic (`default function`)

This function is executed **for every request**.

---

### Target endpoint

```js
const url = 'http://localhost:8080/api/orders/with-headers';
```

---

### Randomized payload (real-world behavior)

```js
const quantity = Math.floor(Math.random() * 5) + 1;
const pricePerItem = 500;

const payload = JSON.stringify({
  customerId: Math.floor(Math.random() * 10000),
  productId: Math.floor(Math.random() * 20) + 1,
  quantity,
  totalAmount: quantity * pricePerItem,
});
```

Why this matters:

* Avoids **duplicate orders**
* Triggers real DB inserts
* Produces **unique Kafka messages**
* Exposes index / locking / transaction issues

---

## Headers

```js
const params = {
  headers: {
    'Content-Type': 'application/json',
  },
};
```

Same as your Postman / curl request.

---

## Send the request

```js
const res = http.post(url, payload, params);
```

Each call:

* Creates an order
* Commits DB transaction
* Produces Kafka event (in your setup)

---

## Validate response

```js
check(res, {
  'status is 200 or 201': (r) =>
    r.status === 200 || r.status === 201,
});
```

* Ensures API didn’t silently fail
* Failed checks show up clearly in k6 summary

---

## Think time

```js
sleep(0.2);
```

* Simulates small user delay
* Arrival-rate still controls RPS

---

##  What this test is REALLY validating (end-to-end)

| Layer    | What gets tested          |
| -------- | ------------------------- |
| API      | Latency, error handling   |
| DB       | Connection pool, locks    |
| Kafka    | Producer throughput       |
| Consumer | Lag under pressure        |
| Infra    | CPU, memory, thread usage |

---

##  Mental model to remember

> **“This script is a traffic generator, SLA enforcer, and chaos detector in one file.”**


